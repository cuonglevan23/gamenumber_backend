package com.numbergame.gamenumber.controller;

import com.numbergame.gamenumber.dto.request.GuessRequest;
import com.numbergame.gamenumber.dto.response.ApiResponse;
import com.numbergame.gamenumber.dto.response.GameHistoryResponse;
import com.numbergame.gamenumber.dto.response.GuessResponse;
import com.numbergame.gamenumber.service.IGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final IGameService gameService;

    @PostMapping("/guess")
    public ResponseEntity<ApiResponse<GuessResponse>> guess(
            @Valid @RequestBody GuessRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        GuessResponse response = gameService.guessNumber(username, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<GameHistoryResponse>>> getHistory(
            Authentication authentication) {
        String username = authentication.getName();
        List<GameHistoryResponse> history = gameService.getGameHistory(username);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
