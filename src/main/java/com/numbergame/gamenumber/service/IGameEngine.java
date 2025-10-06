package com.numbergame.gamenumber.service;

/**
 * Game Engine Interface - Core game logic with Biased RNG + Pity System
 * Industry-standard algorithm for optimal user experience
 */
public interface IGameEngine {

    /**
     * Process a game guess with advanced RNG algorithm
     *
     * @param userId User ID for tracking loss streak
     * @param guessedNumber Number guessed by user
     * @return true if user wins, false otherwise
     */
    boolean processGuess(Long userId, Integer guessedNumber);

    /**
     * Process a game guess with custom win probability
     *
     * @param userId User ID for tracking loss streak
     * @param guessedNumber Number guessed by user
     * @param customWinRate Custom win probability (0.01 to 1.0), null to use default
     * @return true if user wins, false otherwise
     */
    boolean processGuess(Long userId, Integer guessedNumber, Double customWinRate);

    /**
     * Get current loss streak for a user
     *
     * @param userId User ID
     * @return Current loss streak count
     */
    int getLossStreak(Long userId);

    /**
     * Reset loss streak for a user
     *
     * @param userId User ID
     */
    void resetLossStreak(Long userId);

    /**
     * Get adjusted win rate based on loss streak
     *
     * @param userId User ID
     * @return Adjusted win rate (0.0 to 1.0)
     */
    double getAdjustedWinRate(Long userId);
}
