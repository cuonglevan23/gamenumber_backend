package com.numbergame.gamenumber.mapper;

import com.numbergame.gamenumber.dto.response.GameHistoryResponse;
import com.numbergame.gamenumber.entity.GameHistory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GameHistoryMapper {

    public GameHistoryResponse toResponse(GameHistory gameHistory) {
        return GameHistoryResponse.builder()
                .id(gameHistory.getId())
                .guessedNumber(gameHistory.getGuessedNumber())
                .actualNumber(gameHistory.getActualNumber())
                .isCorrect(gameHistory.getIsCorrect())
                .scoreEarned(gameHistory.getScoreEarned())
                .playedAt(gameHistory.getPlayedAt())
                .build();
    }

    public List<GameHistoryResponse> toResponseList(List<GameHistory> gameHistories) {
        return gameHistories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}

