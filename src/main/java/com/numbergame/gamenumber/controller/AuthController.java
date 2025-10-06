package com.numbergame.gamenumber.controller;

import com.numbergame.gamenumber.dto.request.LogoutRequest;
import com.numbergame.gamenumber.dto.request.RegisterRequest;
import com.numbergame.gamenumber.dto.request.SignInRequest;
import com.numbergame.gamenumber.dto.response.ApiResponse;
import com.numbergame.gamenumber.dto.response.RefreshTokenResponse;
import com.numbergame.gamenumber.dto.response.SignInResponse;
import com.numbergame.gamenumber.service.IAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller with JWT + HttpOnly Cookie + Refresh Token Rotation
 *
 * Flow:
 * 1. /sign-in: Generate access token (15 min) + refresh token (7 days) in cookie
 * 2. /refresh-token: Rotate refresh token + generate new access token
 * 3. /logout: Revoke tokens + clear cookie
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * Register new user
     * Returns: Access token in response body + Refresh token in HttpOnly cookie
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SignInResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        SignInResponse result = authService.register(request, response);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", result));
    }

    /**
     * Sign in user
     * Returns: Access token (TTL 15 min) in response body
     * Sets: Refresh token (TTL 7 days) in HttpOnly, Secure, SameSite=Strict cookie
     */
    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<SignInResponse>> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletResponse response) {

        SignInResponse result = authService.signIn(request, response);

        return ResponseEntity.ok(ApiResponse.success("Sign in successful", result));
    }

    /**
     * Refresh access token
     * Takes: Refresh token from HttpOnly cookie
     * Returns: New access token
     * Sets: New refresh token in cookie (rotation - old token is revoked)
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @CookieValue(name = "refreshToken") String refreshToken,
            HttpServletResponse response) {

        RefreshTokenResponse result = authService.refreshToken(refreshToken, response);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", result));
    }

    /**
     * Logout user
     * Actions:
     * 1. Blacklist access token in Redis
     * 2. Revoke all refresh tokens for user
     * 3. Clear HttpOnly cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletResponse response) {

        authService.signOut(request, response);

        return ResponseEntity.ok(ApiResponse.success("Sign out successful", null));
    }
}
