package com.numbergame.gamenumber.exception.custom;

public class InsufficientTurnsException extends RuntimeException {
    public InsufficientTurnsException(String message) {
        super(message);
    }

    public InsufficientTurnsException() {
        super("Insufficient turns. Please buy more turns to continue playing.");
    }
}

