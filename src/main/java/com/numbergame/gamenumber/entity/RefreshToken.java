package com.numbergame.gamenumber.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Refresh Token entity stored in MySQL for persistence
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expiry_date", columnList = "expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
