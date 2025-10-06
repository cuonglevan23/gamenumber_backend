package com.numbergame.gamenumber.entity;

import com.numbergame.gamenumber.enums.GameEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit log entity for tracking game events and user activities
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_user_event_time", columnList = "user_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private GameEventType eventType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

