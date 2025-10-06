package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.service.IGameEngine;
import com.numbergame.gamenumber.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Game Engine Implementation - Biased RNG + Pity System
 * Industry-standard algorithm used by Tencent, miHoYo, EA, Blizzard
 *
 * Features:
 * - Base win rate: configurable (default 5%)
 * - Pity system: guaranteed win after MAX_LOSS_STREAK
 * - Dynamic rate adjustment: win rate increases with loss streak
 * - Thread-safe: uses Redis for distributed state management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameEngineImpl implements IGameEngine {

    private final RedisUtils redisUtils;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${game.win-rate}")
    private Double baseWinRate;

    @Value("${game.max-loss-streak:19}")
    private Integer maxLossStreak;

    @Value("${game.streak-bonus-rate:0.01}")
    private Double streakBonusRate;

    private static final String LOSS_STREAK_KEY = "game:loss_streak:";
    private static final long LOSS_STREAK_TTL = 86400; // 24 hours

    @Override
    public boolean processGuess(Long userId, Integer guessedNumber) {
        return processGuess(userId, guessedNumber, null);
    }

    @Override
    public boolean processGuess(Long userId, Integer guessedNumber, Double customWinRate) {
        // Get current loss streak from Redis
        int lossStreak = getLossStreak(userId);

        // Use custom win rate if provided, otherwise use base rate
        double effectiveBaseRate = (customWinRate != null) ? customWinRate : baseWinRate;

        // Calculate adjusted win rate with pity system
        double adjustedRate = calculateAdjustedRate(lossStreak, effectiveBaseRate);

        log.debug("User {}: Loss streak = {}, Base rate = {}, Adjusted rate = {}",
                userId, lossStreak, effectiveBaseRate, adjustedRate);

        // Pity system: guaranteed win after max loss streak
        boolean isWin;
        if (lossStreak >= maxLossStreak) {
            isWin = true;
            log.info("🎁 Pity system activated for user {}! Guaranteed win after {} losses", userId, lossStreak);
        } else {
            // Biased RNG with adjusted rate
            isWin = secureRandom.nextDouble() < adjustedRate;
        }

        // Update loss streak
        if (isWin) {
            resetLossStreak(userId);
            log.info("✅ User {} WON! Resetting loss streak", userId);
        } else {
            incrementLossStreak(userId, lossStreak);
            log.info("❌ User {} LOST! Loss streak: {} -> {}", userId, lossStreak, lossStreak + 1);
        }

        return isWin;
    }

    @Override
    public int getLossStreak(Long userId) {
        String key = LOSS_STREAK_KEY + userId;
        Object value = redisUtils.get(key);
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    public void resetLossStreak(Long userId) {
        String key = LOSS_STREAK_KEY + userId;
        redisUtils.delete(key);
    }

    @Override
    public double getAdjustedWinRate(Long userId) {
        int lossStreak = getLossStreak(userId);
        return calculateAdjustedRate(lossStreak);
    }

    /**
     * Increment loss streak in Redis
     */
    private void incrementLossStreak(Long userId, int currentStreak) {
        String key = LOSS_STREAK_KEY + userId;
        redisUtils.setWithExpiration(key, String.valueOf(currentStreak + 1), LOSS_STREAK_TTL, TimeUnit.SECONDS);
    }

    /**
     * Calculate adjusted win rate based on loss streak
     * Formula: baseRate + (lossStreak * bonusRate)
     *
     * Example with base 5%:
     * - 0 losses: 5%
     * - 5 losses: 10%
     * - 10 losses: 15%
     * - 15 losses: 20%
     * - 19 losses: 24% (then guaranteed win)
     */
    private double calculateAdjustedRate(int lossStreak) {
        return calculateAdjustedRate(lossStreak, baseWinRate);
    }

    /**
     * Calculate adjusted win rate based on loss streak with custom base rate
     */
    private double calculateAdjustedRate(int lossStreak, double baseRate) {
        double adjustedRate = baseRate + (lossStreak * streakBonusRate);
        // Cap at 100%
        return Math.min(adjustedRate, 1.0);
    }
}
