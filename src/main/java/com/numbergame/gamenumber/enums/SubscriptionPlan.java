package com.numbergame.gamenumber.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum SubscriptionPlan {
    MONTHLY("monthly", 100, 30, new BigDecimal("9.99")),      // 100 lượt/tháng - $9.99
    QUARTERLY("quarterly", 350, 90, new BigDecimal("24.99")), // 350 lượt/3 tháng - $24.99
    YEARLY("yearly", 1500, 365, new BigDecimal("89.99"));     // 1500 lượt/năm - $89.99

    private final String planName;
    private final int turns;
    private final int durationDays;
    private final BigDecimal price;

    public static SubscriptionPlan fromString(String plan) {
        for (SubscriptionPlan p : values()) {
            if (p.planName.equalsIgnoreCase(plan)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Invalid subscription plan: " + plan);
    }
}
