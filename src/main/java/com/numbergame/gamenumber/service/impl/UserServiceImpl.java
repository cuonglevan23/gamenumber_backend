package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.dto.response.LeaderboardResponse;
import com.numbergame.gamenumber.dto.response.TransactionResponse;
import com.numbergame.gamenumber.dto.response.UserInfoResponse;
import com.numbergame.gamenumber.entity.Transaction;
import com.numbergame.gamenumber.entity.User;
import com.numbergame.gamenumber.enums.SubscriptionPlan;
import com.numbergame.gamenumber.exception.custom.ResourceNotFoundException;
import com.numbergame.gamenumber.mapper.TransactionMapper;
import com.numbergame.gamenumber.mapper.UserMapper;
import com.numbergame.gamenumber.repository.GameHistoryRepository;
import com.numbergame.gamenumber.repository.TransactionRepository;
import com.numbergame.gamenumber.repository.UserRepository;
import com.numbergame.gamenumber.service.ILeaderboardService;
import com.numbergame.gamenumber.service.IRedisService;
import com.numbergame.gamenumber.service.IStripeService;
import com.numbergame.gamenumber.service.IUserService;
import com.numbergame.gamenumber.utils.GameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final UserMapper userMapper;
    private final TransactionMapper transactionMapper;
    private final ILeaderboardService leaderboardService;
    private final IRedisService redisService;
    private final IStripeService stripeService;

    @Value("${game.turns-per-purchase}")
    private Integer turnsPerPurchase;

    @Override
    public UserInfoResponse getUserInfo(String username) {
        log.info("Fetching user info for: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // ⚡ Try cache first
        UserInfoResponse cachedInfo = redisService.getCachedUserInfo(user.getId());
        if (cachedInfo != null) {
            log.debug("Cache HIT for user info: {}", username);
            return cachedInfo;
        }

        // Cache MISS - build from Redis/DB
        log.debug("Cache MISS for user info: {}, building...", username);

        // Get score and turns from Redis (hot data)
        Integer score = redisService.getUserScore(user.getId());
        Integer turns = redisService.getUserTurns(user.getId());

        // Use high-performance leaderboard service (O(log N))
        Long rank = leaderboardService.getUserPosition(user.getId());
        if (rank == null) {
            // Fallback to DB count if not in leaderboard
            rank = userRepository.countUsersWithScoreGreaterThan(score) + 1;
        }

        // Build response
        UserInfoResponse response = UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .score(score)
                .turns(turns)
                .rank(rank)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();

        // Cache for next time
        redisService.cacheUserInfo(user.getId(), response);

        return response;
    }

    @Override
    @Transactional
    public TransactionResponse buyTurns(String username, Integer quantity) {
        log.info("User {} buying {} turn packages", username, quantity);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        int turnsToAdd = turnsPerPurchase * quantity;

        // ⚡ Add turns in Redis (fast, atomic)
        redisService.addTurns(user.getId(), turnsToAdd);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .userId(user.getId())
                .transactionType("PURCHASE")
                .turnsAdded(turnsToAdd)
                .amount(BigDecimal.valueOf(quantity * 1.0)) // $1 per package
                .paymentMethod("DIRECT")
                .paymentStatus("COMPLETED")
                .transactionRef(GameUtils.generateTransactionRef())
                .build();
        transactionRepository.save(transaction);

        // Invalidate user cache
        redisService.invalidateUserCache(user.getId());

        Integer newTurns = redisService.getUserTurns(user.getId());
        log.info("Purchase successful - User: {}, Turns added: {}, Total turns: {}",
                username, turnsToAdd, newTurns);

        return transactionMapper.toResponse(transaction);
    }

    @Override
    // Removed @Cacheable - data is already cached in Redis Sorted Set
    public List<LeaderboardResponse> getLeaderboard() {
        log.info("Fetching leaderboard");

        // ⚡ Use high-performance Redis Sorted Set leaderboard (O(log N))
        List<LeaderboardResponse> leaderboard = redisService.getTopLeaderboard(10);

        // If Redis is empty or has insufficient data, populate from database
        if (leaderboard == null || leaderboard.isEmpty()) {
            log.warn("⚠️ Redis leaderboard is empty, populating from database...");
            int populatedCount = leaderboardService.populateFromDatabase();
            log.info("✅ Populated {} users to Redis leaderboard", populatedCount);

            // Retry getting from Redis after population
            leaderboard = redisService.getTopLeaderboard(10);
        }

        return leaderboard != null ? leaderboard : List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String username) {
        log.info("Fetching transaction history for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return transactionMapper.toResponseList(transactions);
    }

    @Override
    public void updateLeaderboardCache(String username, Integer score) {
        // This method is deprecated - LeaderboardService handles updates automatically
        log.debug("updateLeaderboardCache called for: {} - delegating to LeaderboardService", username);
    }

    @Override
    public String buyTurnsWithStripe(String username, String plan) {
        log.info("User {} initiating Stripe payment for plan: {}", username, plan);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Parse subscription plan
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(plan);

        // Create Stripe checkout session
        String checkoutUrl = stripeService.createCheckoutSession(user.getId(), username, subscriptionPlan);

        log.info("Stripe checkout URL created for user {} - Plan: {}", username, plan);

        return checkoutUrl;
    }
}
