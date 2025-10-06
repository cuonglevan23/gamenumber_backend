package com.numbergame.gamenumber.enums;

/**
 * Transaction types for turn purchases and rewards
 */
public enum TransactionType {
    PURCHASE("Purchase"),
    REWARD("Reward"),
    BONUS("Bonus"),
    REFUND("Refund");
    
    private final String displayName;
    
    TransactionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

