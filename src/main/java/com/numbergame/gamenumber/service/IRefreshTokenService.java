package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.entity.RefreshToken;

/**
 * Service for Refresh Token management with Redis caching
 */
public interface IRefreshTokenService {

    /**
     * Create refresh token for user
     * @param userId User ID
     * @return RefreshToken entity
     */
    RefreshToken createRefreshToken(Long userId);

    /**
     * Verify and get refresh token
     * @param token Refresh token string
     * @return RefreshToken entity
     */
    RefreshToken verifyRefreshToken(String token);

    /**
     * Rotate refresh token (revoke old, create new)
     * @param oldToken Old refresh token
     * @param userId User ID
     * @return New RefreshToken entity
     */
    RefreshToken rotateRefreshToken(String oldToken, Long userId);

    /**
     * Revoke all user's refresh tokens
     * @param userId User ID
     */
    void revokeAllUserTokens(Long userId);

    /**
     * Revoke specific refresh token
     * @param token Refresh token
     */
    void revokeToken(String token);

    /**
     * Clean up expired tokens (scheduled task)
     */
    void cleanupExpiredTokens();
}

