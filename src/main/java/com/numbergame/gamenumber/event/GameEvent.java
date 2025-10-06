package com.numbergame.gamenumber.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base event for all Kafka events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEvent implements Serializable {
    private String eventId;
    private String eventType;
    private Long userId;
    private String username;
    private Object payload;
    private LocalDateTime timestamp;
}

