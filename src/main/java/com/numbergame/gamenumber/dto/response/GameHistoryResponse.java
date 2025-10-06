package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameHistoryResponse {
    private Long id;
    private Integer guessedNumber;
    private Integer actualNumber;
    private Boolean isCorrect;
    private Integer scoreEarned;
    private LocalDateTime playedAt;
}

