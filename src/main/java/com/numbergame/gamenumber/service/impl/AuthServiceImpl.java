package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.config.JwtTokenProvider;
import com.numbergame.gamenumber.dto.request.LogoutRequest;
import com.numbergame.gamenumber.dto.request.RegisterRequest;
import com.numbergame.gamenumber.dto.request.SignInRequest;
import com.numbergame.gamenumber.dto.response.RefreshTokenResponse;
import com.numbergame.gamenumber.dto.response.SignInResponse;
import com.numbergame.gamenumber.entity.RefreshToken;
import com.numbergame.gamenumber.entity.User;
import com.numbergame.gamenumber.enums.GameEventType;
import com.numbergame.gamenumber.event.GameEvent;
import com.numbergame.gamenumber.exception.custom.DuplicateResourceException;
import com.numbergame.gamenumber.exception.custom.InvalidCredentialsException;
import com.numbergame.gamenumber.repository.UserRepository;
import com.numbergame.gamenumber.service.IAuditService;
import com.numbergame.gamenumber.service.IAuthService;
import com.numbergame.gamenumber.service.IEventPublisher;
import com.numbergame.gamenumber.service.IRefreshTokenService;
import com.numbergame.gamenumber.service.ITokenBlacklistService;
import com.numbergame.gamenumber.utils.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final IAuditService auditService;
    private final IEventPublisher eventPublisher;
    private final IRefreshTokenService refreshTokenService;
    private final ITokenBlacklistService tokenBlacklistService;
    private final CookieUtils cookieUtils;

    @Value("${game.default-turns}")
    private Integer defaultTurns;

    @Value("${jwt.expiration}")
    private Long accessTokenExpiration;

    @Override
    @Transactional
    public SignInResponse register(RegisterRequest request, HttpServletResponse response) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        // Check if email exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .score(0)
                .turns(defaultTurns)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // Set refresh token in HttpOnly cookie
        cookieUtils.addRefreshTokenCookie(response, refreshToken.getToken());

        // Audit logging (async)
        auditService.logEvent(user.getId(), user.getUsername(),
                GameEventType.USER_REGISTERED,
                "New user registered");

        // Publish event to Kafka
        GameEvent event = GameEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(GameEventType.USER_REGISTERED.name())
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishUserEvent(event);

        return SignInResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .username(user.getUsername())
                .email(user.getEmail())
                .score(user.getScore())
                .turns(user.getTurns())
                .build();
    }

    @Override
    @Transactional
    public SignInResponse signIn(SignInRequest request, HttpServletResponse response) {
        log.info("User sign in attempt: {}", request.getUsername());

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.getUsername());
            throw new InvalidCredentialsException();
        }

        // Get user and update last login
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException());

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // Set refresh token in HttpOnly cookie
        cookieUtils.addRefreshTokenCookie(response, refreshToken.getToken());

        log.info("User signed in successfully: {}", user.getUsername());

        // Audit logging
        auditService.logEvent(user.getId(), user.getUsername(),
                GameEventType.USER_LOGIN,
                "User signed in");

        // Publish event to Kafka
        GameEvent event = GameEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(GameEventType.USER_LOGIN.name())
                .userId(user.getId())
                .username(user.getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishUserEvent(event);

        return SignInResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .username(user.getUsername())
                .email(user.getEmail())
                .score(user.getScore())
                .turns(user.getTurns())
                .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshTokenStr, HttpServletResponse response) {
        log.info("Refreshing access token");

        // Verify refresh token
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenStr);

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // Rotate refresh token (revoke old, create new)
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                refreshTokenStr, user.getId());

        // Generate new access token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtTokenProvider.generateToken(userDetails);

        // Update cookie with new refresh token
        cookieUtils.addRefreshTokenCookie(response, newRefreshToken.getToken());

        log.info("Token refreshed for user: {}", user.getUsername());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .build();
    }

    @Override
    @Transactional
    public void signOut(LogoutRequest request, HttpServletResponse response) {
        log.info("User sign out");

        // Extract username from access token
        String username = jwtTokenProvider.extractUsername(request.getAccessToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        // Blacklist access token
        long remainingTime = jwtTokenProvider.extractExpiration(request.getAccessToken()).getTime()
                - System.currentTimeMillis();
        if (remainingTime > 0) {
            tokenBlacklistService.blacklistToken(request.getAccessToken(), remainingTime / 1000);
        }

        // Revoke all refresh tokens for this user
        refreshTokenService.revokeAllUserTokens(user.getId());

        // Clear cookie
        cookieUtils.clearRefreshTokenCookie(response);

        // Audit logging
        auditService.logEvent(user.getId(), user.getUsername(),
                GameEventType.USER_LOGOUT,
                "User signed out");

        log.info("User signed out: {}", username);
    }
}
