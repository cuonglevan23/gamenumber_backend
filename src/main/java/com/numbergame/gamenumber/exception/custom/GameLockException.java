package com.numbergame.gamenumber.exception.custom;

public class GameLockException extends RuntimeException {
    public GameLockException(String message) {
        super(message);
    }

    public GameLockException() {
        super("Another guess is being processed. Please wait a moment and try again.");
    }
}

