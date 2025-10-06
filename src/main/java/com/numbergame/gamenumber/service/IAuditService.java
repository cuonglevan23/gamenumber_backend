package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.enums.GameEventType;

/**
 * Service for audit logging
 */
public interface IAuditService {

    /**
     * Log game event
     * @param userId User ID
     * @param username Username
     * @param eventType Event type
     * @param description Event description
     */
    void logEvent(Long userId, String username, GameEventType eventType, String description);

    /**
     * Log event with metadata
     * @param userId User ID
     * @param username Username
     * @param eventType Event type
     * @param description Event description
     * @param metadata Additional metadata as JSON
     */
    void logEventWithMetadata(Long userId, String username, GameEventType eventType, String description, String metadata);

    /**
     * Log event with IP and user agent
     * @param userId User ID
     * @param username Username
     * @param eventType Event type
     * @param description Event description
     * @param ipAddress IP address
     * @param userAgent User agent
     */
    void logEventWithRequest(Long userId, String username, GameEventType eventType, String description, String ipAddress, String userAgent);
}

