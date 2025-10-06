package com.numbergame.gamenumber.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_history", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_played_at", columnList = "played_at"),
    @Index(name = "idx_user_played", columnList = "user_id, played_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "guessed_number", nullable = false)
    private Integer guessedNumber;

    @Column(name = "actual_number", nullable = false)
    private Integer actualNumber;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Builder.Default
    @Column(name = "score_earned", columnDefinition = "INT DEFAULT 0")
    private Integer scoreEarned = 0;

    @CreationTimestamp
    @Column(name = "played_at", updatable = false)
    private LocalDateTime playedAt;
}
