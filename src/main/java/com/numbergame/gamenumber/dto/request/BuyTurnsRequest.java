package com.numbergame.gamenumber.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyTurnsRequest {

    // For simple quantity-based purchase (legacy support)
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // For subscription-based purchase (Stripe)
    @Pattern(regexp = "monthly|quarterly|yearly", message = "Plan must be monthly, quarterly, or yearly")
    private String plan;

    // Payment method: "direct" (legacy) or "stripe"
    @NotNull(message = "Payment method is required")
    @Pattern(regexp = "direct|stripe", message = "Payment method must be 'direct' or 'stripe'")
    private String paymentMethod;
}
