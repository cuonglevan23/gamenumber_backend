package com.numbergame.gamenumber.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuessResponse {
    private Boolean correct;
    private Integer guessedNumber;
    private Integer actualNumber;
    private Integer scoreEarned;
    private Integer totalScore;
    private Integer remainingTurns;
    private String message;
    private Long gameId;
}

