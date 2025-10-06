package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.dto.request.LogoutRequest;
import com.numbergame.gamenumber.dto.request.RegisterRequest;
import com.numbergame.gamenumber.dto.request.SignInRequest;
import com.numbergame.gamenumber.dto.response.RefreshTokenResponse;
import com.numbergame.gamenumber.dto.response.SignInResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService {
    
    /**
     * Register new user
     * @param request Registration data
     * @return Sign in response with access token
     */
    SignInResponse register(RegisterRequest request, HttpServletResponse response);

    /**
     * Sign in user
     * @param request Login credentials
     * @param response HTTP response to set cookie
     * @return Sign in response with access token
     */
    SignInResponse signIn(SignInRequest request, HttpServletResponse response);

    /**
     * Refresh access token using refresh token from cookie
     * @param refreshToken Refresh token from cookie
     * @param response HTTP response to set new cookie
     * @return New access token
     */
    RefreshTokenResponse refreshToken(String refreshToken, HttpServletResponse response);

    /**
     * Logout user
     * @param request Logout request with access token
     * @param response HTTP response to clear cookie
     */
    void signOut(LogoutRequest request, HttpServletResponse response);
}
