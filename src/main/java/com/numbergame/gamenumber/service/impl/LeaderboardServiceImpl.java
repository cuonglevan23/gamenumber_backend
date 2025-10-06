package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.dto.response.LeaderboardResponse;
import com.numbergame.gamenumber.entity.User;
import com.numbergame.gamenumber.repository.UserRepository;
import com.numbergame.gamenumber.service.ILeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance Leaderboard Service using Redis Sorted Set
 *
 * Key features:
 * - O(log N) complexity for most operations
 * - Real-time updates
 * - Handles millions of users efficiently
 * - Persistent in Redis with optional DB sync
 * - Optimized batch loading to prevent N+1 queries
 * - Response caching for ultra-fast retrieval
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceImpl implements ILeaderboardService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    private static final String LEADERBOARD_KEY = "leaderboard:global";
    private static final String USER_DATA_KEY = "leaderboard:user:";
    private static final String LEADERBOARD_CACHE_KEY = "leaderboard:cache:top";
    private static final long LEADERBOARD_CACHE_TTL = 60; // 1 minute cache

    @Override
    public void updateScore(Long userId, String username, Integer score) {
        try {
            // Update score in Redis Sorted Set (O(log N))
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId.toString(), score);

            // Cache user data for quick retrieval using pipeline
            String userDataKey = USER_DATA_KEY + userId;
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<?>) connection -> {
                redisTemplate.opsForHash().put(userDataKey, "username", username);
                redisTemplate.opsForHash().put(userDataKey, "score", score.toString());
                return null;
            });

            // Invalidate leaderboard cache when score updates
            redisTemplate.delete(LEADERBOARD_CACHE_KEY + ":10");

            log.debug("Updated leaderboard: User {} - Score {}", username, score);
        } catch (Exception e) {
            log.error("Failed to update leaderboard for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public List<LeaderboardResponse> getTopUsers(int limit) {
        try {
            // Try to get from cache first (ultra-fast)
            String cacheKey = LEADERBOARD_CACHE_KEY + ":" + limit;
            List<String> cachedData = redisTemplate.opsForList().range(cacheKey, 0, -1);

            if (cachedData != null && !cachedData.isEmpty()) {
                log.debug("Leaderboard cache HIT for top {}", limit);
                return deserializeLeaderboard(cachedData);
            }

            log.debug("Leaderboard cache MISS, building from Redis Sorted Set");

            // Get top N users with highest scores (O(log N + M))
            Set<ZSetOperations.TypedTuple<String>> topUsers =
                redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);

            if (topUsers == null || topUsers.isEmpty()) {
                log.warn("Leaderboard is empty, loading from database");
                return loadLeaderboardFromDB(limit);
            }

            // Extract user IDs for batch loading
            List<Long> userIds = topUsers.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .collect(Collectors.toList());

            // Batch load usernames from Redis using pipeline (prevents N+1 problem)
            Map<Long, String> usernameMap = batchLoadUsernames(userIds);

            // Build leaderboard response
            List<LeaderboardResponse> leaderboard = new ArrayList<>();
            int rank = 1;

            for (ZSetOperations.TypedTuple<String> tuple : topUsers) {
                Long userId = Long.parseLong(tuple.getValue());
                Integer score = tuple.getScore().intValue();
                String username = usernameMap.getOrDefault(userId, "Unknown");

                leaderboard.add(LeaderboardResponse.builder()
                    .rank(rank++)
                    .userId(userId)
                    .username(username)
                    .score(score)
                    .build());
            }

            // Cache the result for ultra-fast subsequent requests
            cacheLeaderboardResponse(cacheKey, leaderboard);

            return leaderboard;
        } catch (Exception e) {
            log.error("Failed to get top users from leaderboard: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public LeaderboardResponse getUserRank(Long userId) {
        try {
            // Get user's score (O(1))
            Double score = redisTemplate.opsForZSet().score(LEADERBOARD_KEY, userId.toString());

            if (score == null) {
                log.debug("User {} not found in leaderboard", userId);
                return null;
            }

            // Get user's rank (O(log N))
            Long rank = redisTemplate.opsForZSet()
                .reverseRank(LEADERBOARD_KEY, userId.toString());

            // Get username from cache
            String username = (String) redisTemplate.opsForHash()
                .get(USER_DATA_KEY + userId, "username");

            if (username == null) {
                username = userRepository.findById(userId)
                    .map(User::getUsername)
                    .orElse("Unknown");
            }

            return LeaderboardResponse.builder()
                .rank(rank != null ? rank.intValue() + 1 : null) // Convert to 1-based rank
                .userId(userId)
                .username(username)
                .score(score.intValue())
                .build();
        } catch (Exception e) {
            log.error("Failed to get user rank for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public Long getUserPosition(Long userId) {
        Long rank = redisTemplate.opsForZSet()
            .reverseRank(LEADERBOARD_KEY, userId.toString());
        return rank != null ? rank + 1 : null; // Convert to 1-based
    }

    @Override
    public void removeUser(Long userId) {
        try {
            redisTemplate.opsForZSet().remove(LEADERBOARD_KEY, userId.toString());
            redisTemplate.delete(USER_DATA_KEY + userId);

            // Invalidate cache
            redisTemplate.keys(LEADERBOARD_CACHE_KEY + ":*")
                .forEach(key -> redisTemplate.delete(key));

            log.info("Removed user {} from leaderboard", userId);
        } catch (Exception e) {
            log.error("Failed to remove user {} from leaderboard: {}", userId, e.getMessage());
        }
    }

    @Override
    public long getTotalUsers() {
        Long total = redisTemplate.opsForZSet().size(LEADERBOARD_KEY);
        return total != null ? total : 0L;
    }

    @Override
    public int populateFromDatabase() {
        log.info("🔄 Populating leaderboard from database...");

        try {
            List<User> allUsers = userRepository.findAll();

            if (allUsers.isEmpty()) {
                log.warn("No users found in database");
                return 0;
            }

            int count = 0;
            for (User user : allUsers) {
                updateScore(user.getId(), user.getUsername(), user.getScore());
                count++;
            }

            log.info("✅ Successfully populated leaderboard with {} users", count);
            return count;

        } catch (Exception e) {
            log.error("❌ Failed to populate leaderboard from database: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Batch load usernames from Redis using pipeline
     * Prevents N+1 query problem - loads all usernames in 1 operation
     * Complexity: O(N) instead of O(N) separate calls
     */
    private Map<Long, String> batchLoadUsernames(List<Long> userIds) {
        Map<Long, String> usernameMap = new HashMap<>();

        if (userIds.isEmpty()) {
            return usernameMap;
        }

        try {
            // Use pipeline for batch loading (1 network roundtrip instead of N)
            List<Object> results = redisTemplate.executePipelined(
                (org.springframework.data.redis.core.RedisCallback<?>) connection -> {
                    for (Long userId : userIds) {
                        redisTemplate.opsForHash().get(USER_DATA_KEY + userId, "username");
                    }
                    return null;
                });

            // Map results back to userIds
            List<Long> missingUserIds = new ArrayList<>();
            for (int i = 0; i < userIds.size(); i++) {
                Long userId = userIds.get(i);
                Object username = results.get(i);

                if (username != null) {
                    usernameMap.put(userId, username.toString());
                } else {
                    missingUserIds.add(userId);
                }
            }

            // Batch load missing usernames from DB (if any)
            if (!missingUserIds.isEmpty()) {
                log.debug("Loading {} missing usernames from DB", missingUserIds.size());
                List<User> users = userRepository.findAllById(missingUserIds);

                for (User user : users) {
                    usernameMap.put(user.getId(), user.getUsername());
                    // Cache for next time
                    redisTemplate.opsForHash().put(
                        USER_DATA_KEY + user.getId(),
                        "username",
                        user.getUsername()
                    );
                }
            }

        } catch (Exception e) {
            log.error("Error in batch loading usernames: {}", e.getMessage());
            // Fallback to individual loading if pipeline fails
            for (Long userId : userIds) {
                String username = (String) redisTemplate.opsForHash()
                    .get(USER_DATA_KEY + userId, "username");
                if (username != null) {
                    usernameMap.put(userId, username);
                }
            }
        }

        return usernameMap;
    }

    /**
     * Cache leaderboard response for ultra-fast retrieval
     * TTL: 1 minute (configurable)
     */
    private void cacheLeaderboardResponse(String cacheKey, List<LeaderboardResponse> leaderboard) {
        try {
            // Serialize leaderboard to simple string format for fast caching
            // Format: rank|userId|username|score
            List<String> serialized = leaderboard.stream()
                .map(lb -> String.format("%d|%d|%s|%d",
                    lb.getRank(), lb.getUserId(), lb.getUsername(), lb.getScore()))
                .collect(Collectors.toList());

            // Store in Redis List with TTL
            redisTemplate.delete(cacheKey);
            if (!serialized.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(cacheKey, serialized);
                redisTemplate.expire(cacheKey, LEADERBOARD_CACHE_TTL, java.util.concurrent.TimeUnit.SECONDS);
            }

            log.debug("Cached leaderboard response with {} entries", leaderboard.size());
        } catch (Exception e) {
            log.error("Failed to cache leaderboard response: {}", e.getMessage());
        }
    }

    /**
     * Deserialize cached leaderboard data
     */
    private List<LeaderboardResponse> deserializeLeaderboard(List<String> cachedData) {
        return cachedData.stream()
            .map(obj -> {
                String[] parts = obj.split("\\|");
                return LeaderboardResponse.builder()
                    .rank(Integer.parseInt(parts[0]))
                    .userId(Long.parseLong(parts[1]))
                    .username(parts[2])
                    .score(Integer.parseInt(parts[3]))
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Fallback: Load leaderboard from database
     * Only used when Redis is empty (first time or after reset)
     */
    private List<LeaderboardResponse> loadLeaderboardFromDB(int limit) {
        log.info("Loading leaderboard from database (fallback)");

        List<User> allUsers = userRepository.findAllByOrderByScoreDesc();
        List<User> topUsers = allUsers.stream()
            .limit(limit)
            .collect(Collectors.toList());

        List<LeaderboardResponse> leaderboard = new ArrayList<>();

        int rank = 1;
        for (User user : topUsers) {
            // Populate Redis while loading
            updateScore(user.getId(), user.getUsername(), user.getScore());

            leaderboard.add(LeaderboardResponse.builder()
                .rank(rank++)
                .userId(user.getId())
                .username(user.getUsername())
                .score(user.getScore())
                .build());
        }

        return leaderboard;
    }
}
