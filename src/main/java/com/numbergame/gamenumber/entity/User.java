package com.numbergame.gamenumber.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_score_desc", columnList = "score DESC"),
    @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer score = 0;
    
    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 5")
    private Integer turns = 5;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Version
    private Long version;
}
