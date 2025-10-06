package com.numbergame.gamenumber.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility for managing HTTP cookies
 */
@Component
public class CookieUtils {

    @Value("${cookie.refresh-token.name:refreshToken}")
    private String cookieName;

    @Value("${cookie.refresh-token.max-age:604800}")
    private int maxAge;

    @Value("${cookie.refresh-token.http-only:true}")
    private boolean httpOnly;

    @Value("${cookie.refresh-token.secure:true}")
    private boolean secure;

    @Value("${cookie.refresh-token.same-site:Strict}")
    private String sameSite;

    @Value("${cookie.refresh-token.path:/}")
    private String path;

    /**
     * Create HttpOnly, Secure cookie for refresh token
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(cookieName, refreshToken);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);

        // Set SameSite attribute via header (not directly supported by Cookie class)
        response.addHeader("Set-Cookie", String.format(
                "%s=%s; Path=%s; Max-Age=%d; HttpOnly; Secure; SameSite=%s",
                cookieName, refreshToken, path, maxAge, sameSite
        ));
    }

    /**
     * Clear refresh token cookie
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(path);
        cookie.setMaxAge(0);

        response.addHeader("Set-Cookie", String.format(
                "%s=; Path=%s; Max-Age=0; HttpOnly; Secure; SameSite=%s",
                cookieName, path, sameSite
        ));
    }
}

