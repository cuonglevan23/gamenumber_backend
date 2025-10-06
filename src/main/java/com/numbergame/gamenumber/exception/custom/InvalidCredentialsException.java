package com.numbergame.gamenumber.exception.custom;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
