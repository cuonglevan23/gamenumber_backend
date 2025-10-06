package com.numbergame.gamenumber.service;

/**
 * Service for JWT token blacklist management using Redis
 */
public interface ITokenBlacklistService {
    
    /**
     * Add token to blacklist
     * @param token JWT token
     * @param expirationSeconds Token expiration in seconds
     */
    void blacklistToken(String token, long expirationSeconds);
    
    /**
     * Check if token is blacklisted
     * @param token JWT token
     * @return true if blacklisted
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * Remove token from blacklist (for testing)
     * @param token JWT token
     */
    void removeFromBlacklist(String token);
}

