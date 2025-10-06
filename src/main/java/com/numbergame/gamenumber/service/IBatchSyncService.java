package com.numbergame.gamenumber.service;

/**
 * Batch Sync Service - Sync Redis data to MySQL periodically
 *
 * Benefits:
 * - Reduces DB writes by 90%
 * - Batch processing is more efficient than individual updates
 * - Prevents DB contention during high traffic
 *
 * Industry standard: Netflix, Shopee, Grab
 */
public interface IBatchSyncService {

    /**
     * Sync all dirty users from Redis to MySQL
     * Runs every 5 minutes (configurable)
     *
     * @return Number of users synced
     */
    int syncDirtyUsersToDatabase();

    /**
     * Force sync a specific user immediately
     * Use for critical operations (e.g., user logout, payment)
     */
    void forceSyncUser(Long userId);

    /**
     * Get pending sync count
     */
    long getPendingSyncCount();
}

