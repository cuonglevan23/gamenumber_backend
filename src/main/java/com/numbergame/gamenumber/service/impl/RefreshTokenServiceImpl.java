package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.entity.RefreshToken;
import com.numbergame.gamenumber.exception.custom.InvalidCredentialsException;
import com.numbergame.gamenumber.repository.RefreshTokenRepository;
import com.numbergame.gamenumber.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private Long refreshTokenDurationMs;

    private static final int MAX_ACTIVE_TOKENS_PER_USER = 5;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Clean up old tokens if too many
        long activeTokens = refreshTokenRepository.countActiveTokensByUserId(userId);
        if (activeTokens >= MAX_ACTIVE_TOKENS_PER_USER) {
            refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
            log.info("Revoked old tokens for user: {} (exceeded max limit)", userId);
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);

        log.debug("Created refresh token for user: {}", userId);
        return refreshToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        // Query from database (no caching - refresh tokens are rarely used)
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        if (refreshToken.isExpired()) {
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken, Long userId) {
        // Verify and revoke old token
        RefreshToken oldRefreshToken = verifyRefreshToken(oldToken);

        if (!oldRefreshToken.getUserId().equals(userId)) {
            throw new InvalidCredentialsException("Token does not belong to user");
        }

        // Revoke old token
        revokeToken(oldToken);

        // Create new token
        RefreshToken newRefreshToken = createRefreshToken(userId);

        log.info("Rotated refresh token for user: {}", userId);
        return newRefreshToken;
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Token not found"));

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        log.debug("Revoked refresh token: {}", token.substring(0, 10) + "...");
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired refresh tokens");
    }
}
