package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.dto.request.GuessRequest;
import com.numbergame.gamenumber.dto.response.GameHistoryResponse;
import com.numbergame.gamenumber.dto.response.GuessResponse;
import com.numbergame.gamenumber.entity.GameHistory;
import com.numbergame.gamenumber.entity.User;
import com.numbergame.gamenumber.enums.GameEventType;
import com.numbergame.gamenumber.event.GameEvent;
import com.numbergame.gamenumber.exception.custom.GameLockException;
import com.numbergame.gamenumber.exception.custom.InsufficientTurnsException;
import com.numbergame.gamenumber.exception.custom.ResourceNotFoundException;
import com.numbergame.gamenumber.mapper.GameHistoryMapper;
import com.numbergame.gamenumber.repository.GameHistoryRepository;
import com.numbergame.gamenumber.repository.UserRepository;
import com.numbergame.gamenumber.service.IAuditService;
import com.numbergame.gamenumber.service.IEventPublisher;
import com.numbergame.gamenumber.service.IGameEngine;
import com.numbergame.gamenumber.service.IGameService;
import com.numbergame.gamenumber.service.ILeaderboardService;
import com.numbergame.gamenumber.service.IRedisService;
import com.numbergame.gamenumber.utils.GameUtils;
import com.numbergame.gamenumber.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameServiceImpl implements IGameService {

    private final UserRepository userRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final GameHistoryMapper gameHistoryMapper;
    private final RedisUtils redisUtils;
    private final IAuditService auditService;
    private final IEventPublisher eventPublisher;
    private final IGameEngine gameEngine;
    private final ILeaderboardService leaderboardService;
    private final IRedisService redisService;

    @Value("${game.min-number}")
    private Integer minNumber;

    @Value("${game.max-number}")
    private Integer maxNumber;

    @Value("${game.win-rate}")
    private Double winRate;

    private static final long LOCK_TIMEOUT = 5;
    private static final int MAX_LOCK_RETRIES = 2;
    private static final long LOCK_RETRY_DELAY = 50;

    @Override
    @Transactional
    @CacheEvict(value = "leaderboard", allEntries = true)
    public GuessResponse guessNumber(String username, GuessRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("🎮 Processing guess for user: {}, number: {}", username, request.getNumber());

        // Validate input
        if (!GameUtils.isValidGuess(request.getNumber(), minNumber, maxNumber)) {
            throw new IllegalArgumentException("Number must be between " + minNumber + " and " + maxNumber);
        }

        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        final Long userId = user.getId();

        // 🔒 Acquire distributed lock
        String lockKey = "game:lock:" + userId;
        if (!acquireLockWithRetry(lockKey)) {
            log.warn("⚠️ Failed to acquire lock for user {}", username);
            throw new GameLockException();
        }

        try {
            // ⚡ Get turns from Redis (HOT DATA)
            Integer currentTurns = redisService.getUserTurns(userId);

            if (currentTurns <= 0) {
                throw new InsufficientTurnsException();
            }

            // ⚡ Deduct turn (atomic)
            redisService.decrementTurns(userId);

            // 🎲 Game Engine (Biased RNG + Pity System)
            // Use custom win probability if provided in request
            boolean isCorrect = gameEngine.processGuess(userId, request.getNumber(), request.getWinProbability());

            // Generate actual number
            int actualNumber;
            if (isCorrect) {
                actualNumber = request.getNumber();
            } else {
                do {
                    actualNumber = GameUtils.generateRandomNumber(minNumber, maxNumber);
                } while (actualNumber == request.getNumber());
            }

            int scoreEarned = 0;
            Integer newScore = redisService.getUserScore(userId);

            if (isCorrect) {
                scoreEarned = 1;
                redisService.incrementScore(userId, scoreEarned);
                newScore = redisService.getUserScore(userId);
            }

            // Save game history
            GameHistory gameHistory = GameHistory.builder()
                    .userId(userId)
                    .guessedNumber(request.getNumber())
                    .actualNumber(actualNumber)
                    .isCorrect(isCorrect)
                    .scoreEarned(scoreEarned)
                    .build();
            gameHistoryRepository.save(gameHistory);

            // ⚡ Update leaderboard for ALL users (not just when correct)
            // This ensures all users appear in the leaderboard
            leaderboardService.updateScore(userId, user.getUsername(), newScore);

            // Clear cache
            redisService.invalidateUserCache(userId);

            // Get stats
            int lossStreak = gameEngine.getLossStreak(userId);
            double adjustedRate = gameEngine.getAdjustedWinRate(userId);

            log.info("✅ Guess processed - User: {}, Correct: {}, Score: {}, Took: {}ms",
                username, isCorrect, newScore, System.currentTimeMillis() - startTime);

            // Publish event (async via Kafka)
            publishGameEvent(userId, user.getUsername(), request.getNumber(), actualNumber,
                isCorrect, scoreEarned, newScore, lossStreak, adjustedRate);

            // Build response
            String message = buildResponseMessage(isCorrect, lossStreak, adjustedRate);

            return GuessResponse.builder()
                    .correct(isCorrect)
                    .guessedNumber(request.getNumber())
                    .actualNumber(actualNumber)
                    .scoreEarned(scoreEarned)
                    .totalScore(newScore)
                    .remainingTurns(currentTurns - 1)
                    .gameId(gameHistory.getId())
                    .message(message)
                    .build();

        } finally {
            redisUtils.releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameHistoryResponse> getGameHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<GameHistory> histories = gameHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId());
        return gameHistoryMapper.toResponseList(histories);
    }

    private boolean acquireLockWithRetry(String lockKey) {
        for (int attempt = 1; attempt <= MAX_LOCK_RETRIES; attempt++) {
            if (redisUtils.acquireLock(lockKey, LOCK_TIMEOUT)) {
                return true;
            }
            if (attempt < MAX_LOCK_RETRIES) {
                try {
                    Thread.sleep(LOCK_RETRY_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void publishGameEvent(Long userId, String username, int guessedNumber,
            int actualNumber, boolean isCorrect, int scoreEarned, int totalScore,
            int lossStreak, double adjustedRate) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("guessedNumber", guessedNumber);
        payload.put("actualNumber", actualNumber);
        payload.put("isCorrect", isCorrect);
        payload.put("scoreEarned", scoreEarned);
        payload.put("totalScore", totalScore);
        payload.put("lossStreak", lossStreak);
        payload.put("adjustedWinRate", adjustedRate);

        GameEventType eventType = isCorrect ? GameEventType.GAME_WON : GameEventType.GAME_LOST;

        GameEvent event = GameEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType.name())
                .userId(userId)
                .username(username)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        // Async publish via Kafka
        eventPublisher.publishGameEvent(event);

        // Audit log
        auditService.logEvent(userId, username, eventType,
            String.format("Guessed %d, Actual: %d, Result: %s",
                guessedNumber, actualNumber, isCorrect ? "WIN" : "LOSS"));
    }

    private String buildResponseMessage(boolean isCorrect, int lossStreak, double adjustedRate) {
        if (isCorrect) {
            if (lossStreak > 10) {
                return "🎉 Finally! You broke the losing streak!";
            }
            return "🎉 Congratulations! You won!";
        } else {
            if (lossStreak >= 15) {
                return String.format("💪 Keep trying! Win chance: %.0f%%", adjustedRate * 100);
            } else if (lossStreak >= 10) {
                return String.format("🎯 Your luck is improving (%.0f%% now)", adjustedRate * 100);
            } else if (lossStreak >= 5) {
                return "💫 Keep playing!";
            }
            return "❌ Try again!";
        }
    }
}
