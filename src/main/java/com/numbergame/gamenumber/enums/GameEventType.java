package com.numbergame.gamenumber.enums;

/**
 * Game event types for audit logging
 */
public enum GameEventType {
    GAME_STARTED("Game Started"),
    GAME_WON("Game Won"),
    GAME_LOST("Game Lost"),
    TURN_PURCHASED("Turn Purchased"),
    USER_REGISTERED("User Registered"),
    USER_LOGIN("User Login"),
    USER_LOGOUT("User Logout"),
    SCORE_UPDATED("Score Updated");

    private final String displayName;

    GameEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

