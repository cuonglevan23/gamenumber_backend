package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.dto.response.LeaderboardResponse;
import com.numbergame.gamenumber.dto.response.TransactionResponse;
import com.numbergame.gamenumber.dto.response.UserInfoResponse;

import java.util.List;

public interface IUserService {
    
    /**
     * Get user information with caching
     * @param username Username
     * @return User info with rank
     */
    UserInfoResponse getUserInfo(String username);
    
    /**
     * Buy turns for user
     * @param username Username
     * @param quantity Number of turn packages to buy
     * @return Transaction response
     */
    TransactionResponse buyTurns(String username, Integer quantity);
    
    /**
     * Buy turns with Stripe subscription
     * @param username Username
     * @param plan Subscription plan (monthly, quarterly, yearly)
     * @return Stripe checkout URL
     */
    String buyTurnsWithStripe(String username, String plan);

    /**
     * Get leaderboard with Redis caching
     * @return Top 10 players
     */
    List<LeaderboardResponse> getLeaderboard();
    
    /**
     * Get transaction history for user
     * @param username Username
     * @return List of transactions
     */
    List<TransactionResponse> getTransactionHistory(String username);
    
    /**
     * Update leaderboard cache
     * @param username Username
     * @param score Score to update
     */
    void updateLeaderboardCache(String username, Integer score);
}
