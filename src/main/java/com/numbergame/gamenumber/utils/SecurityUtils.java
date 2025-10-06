package com.numbergame.gamenumber.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Utility class for Spring Security operations
 */
@Component
public class SecurityUtils {
    
    /**
     * Get the current authenticated username
     * @return username or null if not authenticated
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        return null;
    }
    
    /**
     * Get the current authentication object
     * @return Optional of Authentication
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }
    
    /**
     * Check if current user is authenticated
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * Get UserDetails from current authentication
     * @return Optional of UserDetails
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        return getCurrentAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserDetails)
                .map(principal -> (UserDetails) principal);
    }
    
    /**
     * Extract username from JWT token
     * @param token JWT token
     * @param secret JWT secret key
     * @return username
     */
    public static String extractUsernameFromToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
