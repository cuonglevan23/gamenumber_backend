package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.dto.request.GuessRequest;
import com.numbergame.gamenumber.dto.response.GameHistoryResponse;
import com.numbergame.gamenumber.dto.response.GuessResponse;

import java.util.List;

public interface IGameService {
    
    /**
     * Process number guess with distributed locking
     * @param username Username of player
     * @param request Guess data
     * @return Guess result with score update
     */
    GuessResponse guessNumber(String username, GuessRequest request);
    
    /**
     * Get game history for user
     * @param username Username
     * @return List of game history
     */
    List<GameHistoryResponse> getGameHistory(String username);
}

