package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.entity.User;
import com.numbergame.gamenumber.repository.UserRepository;
import com.numbergame.gamenumber.service.IBatchSyncService;
import com.numbergame.gamenumber.service.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Batch Sync Service - Production-grade implementation
 *
 * Strategy:
 * - Collect all dirty users (users with pending updates in Redis)
 * - Batch update to MySQL every 5 minutes
 * - Use batch processing to reduce DB round-trips
 * - Handle failures gracefully
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchSyncServiceImpl implements IBatchSyncService {

    private final IRedisService redisService;
    private final UserRepository userRepository;

    /**
     * Scheduled batch sync: Every 5 minutes
     * Cron expression: 0 STAR/5 * * * * (every 5 minutes)
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Async
    public void scheduledSync() {
        log.info("🔄 Starting scheduled batch sync...");
        long startTime = System.currentTimeMillis();

        int syncedCount = syncDirtyUsersToDatabase();

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Batch sync completed: {} users synced in {}ms", syncedCount, duration);
    }

    @Override
    @Transactional
    public int syncDirtyUsersToDatabase() {
        Set<Long> dirtyUsers = redisService.getDirtyUsers();

        if (dirtyUsers.isEmpty()) {
            log.debug("No dirty users to sync");
            return 0;
        }

        log.info("Syncing {} dirty users to database", dirtyUsers.size());

        List<User> usersToUpdate = new ArrayList<>();
        int successCount = 0;

        for (Long userId : dirtyUsers) {
            try {
                // Get latest data from Redis
                Integer score = redisService.getUserScore(userId);
                Integer turns = redisService.getUserTurns(userId);

                // Load user from DB
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("User {} not found in database, skipping sync", userId);
                    redisService.clearDirtyFlag(userId);
                    continue;
                }

                // Update user data
                user.setScore(score);
                user.setTurns(turns);
                usersToUpdate.add(user);

                successCount++;

                // Clear dirty flag after successful sync
                redisService.clearDirtyFlag(userId);

            } catch (Exception e) {
                log.error("Failed to sync user {}: {}", userId, e.getMessage());
            }
        }

        // Batch save to DB
        if (!usersToUpdate.isEmpty()) {
            userRepository.saveAll(usersToUpdate);
            log.info("Batch saved {} users to database", usersToUpdate.size());
        }

        return successCount;
    }

    @Override
    @Transactional
    public void forceSyncUser(Long userId) {
        log.info("Force syncing user {}", userId);

        try {
            Integer score = redisService.getUserScore(userId);
            Integer turns = redisService.getUserTurns(userId);

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setScore(score);
                user.setTurns(turns);
                userRepository.save(user);

                redisService.clearDirtyFlag(userId);
                log.info("Force sync completed for user {}", userId);
            }
        } catch (Exception e) {
            log.error("Force sync failed for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public long getPendingSyncCount() {
        return redisService.getDirtyUsers().size();
    }
}
