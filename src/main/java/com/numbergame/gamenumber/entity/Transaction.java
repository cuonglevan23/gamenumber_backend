package com.numbergame.gamenumber.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_transactions", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_transaction_type", columnList = "transaction_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // PURCHASE, REWARD, BONUS

    @Column(name = "turns_added", nullable = false)
    private Integer turnsAdded;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Builder.Default
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "COMPLETED";

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "stripe_session_id", length = 255)
    private String stripeSessionId;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan; // monthly, quarterly, yearly

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
