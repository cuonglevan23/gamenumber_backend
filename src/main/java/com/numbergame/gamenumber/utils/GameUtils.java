package com.numbergame.gamenumber.utils;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utility class for game-related operations
 * Optimized for high-performance concurrent access
 */
public class GameUtils {

    // Thread-safe SecureRandom instance (better than Math.random())
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generate random number within range
     * Thread-safe and high-performance
     *
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return Random number
     */
    public static int generateRandomNumber(int min, int max) {
        // Use SecureRandom instead of Math.random() for better thread safety
        return SECURE_RANDOM.nextInt(max - min + 1) + min;
    }

    /**
     * Calculate win rate percentage
     *
     * @param correctGuesses Number of correct guesses
     * @param totalGuesses Total number of guesses
     * @return Win rate as percentage (0-100)
     */
    public static Double calculateWinRate(Long correctGuesses, Long totalGuesses) {
        if (totalGuesses == null || totalGuesses == 0) {
            return 0.0;
        }
        return (correctGuesses * 100.0) / totalGuesses;
    }

    /**
     * Generate unique transaction reference
     * Optimized for performance
     *
     * @return Transaction reference string
     */
    public static String generateTransactionRef() {
        // Use timestamp + short UUID for uniqueness
        return "TXN-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Validate number is within game range
     * Fast inline validation
     *
     * @param number Number to validate
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return true if valid
     */
    public static boolean isValidGuess(Integer number, Integer min, Integer max) {
        return number != null && number >= min && number <= max;
    }

    /**
     * Calculate score multiplier based on streak
     * For future enhancement
     *
     * @param streak Current winning/losing streak
     * @return Score multiplier (1.0 = no bonus)
     */
    public static double calculateScoreMultiplier(int streak) {
        // Could add streak bonuses here in the future
        return 1.0 + (Math.min(streak, 10) * 0.1); // Max 2x multiplier at 10 streak
    }
}
