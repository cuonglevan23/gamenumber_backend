package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionType;
    private Integer turnsAdded;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionRef;
    private String stripeSessionId;
    private String subscriptionPlan; // monthly, quarterly, yearly
    private LocalDateTime createdAt;
}
