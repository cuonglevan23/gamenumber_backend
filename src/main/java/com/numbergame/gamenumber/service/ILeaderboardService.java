package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.dto.response.LeaderboardResponse;

import java.util.List;

/**
 * Leaderboard Service Interface - High-performance leaderboard operations
 * Uses Redis Sorted Set for O(log N) complexity
 */
public interface ILeaderboardService {

    /**
     * Update user score in leaderboard
     * Complexity: O(log N)
     *
     * @param userId User ID
     * @param username Username
     * @param score New score
     */
    void updateScore(Long userId, String username, Integer score);

    /**
     * Get top N users from leaderboard
     * Complexity: O(log N + M) where M is the number of results
     *
     * @param limit Number of top users to retrieve
     * @return List of leaderboard entries
     */
    List<LeaderboardResponse> getTopUsers(int limit);

    /**
     * Get user's rank and position in leaderboard
     * Complexity: O(log N)
     *
     * @param userId User ID
     * @return User's leaderboard info or null if not found
     */
    LeaderboardResponse getUserRank(Long userId);

    /**
     * Get user's position (rank number)
     * Complexity: O(log N)
     *
     * @param userId User ID
     * @return Rank position (1-based) or null if not found
     */
    Long getUserPosition(Long userId);

    /**
     * Remove user from leaderboard
     *
     * @param userId User ID
     */
    void removeUser(Long userId);

    /**
     * Get total number of users in leaderboard
     *
     * @return Total count
     */
    long getTotalUsers();

    /**
     * Populate leaderboard from database
     * Used to sync Redis with database state
     *
     * @return Number of users populated
     */
    int populateFromDatabase();
}
