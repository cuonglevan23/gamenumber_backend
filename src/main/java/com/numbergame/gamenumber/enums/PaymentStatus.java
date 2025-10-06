package com.numbergame.gamenumber.enums;

/**
 * Payment status for transactions
 */
public enum PaymentStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    CANCELLED("Cancelled");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

