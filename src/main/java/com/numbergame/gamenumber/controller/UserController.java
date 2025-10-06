package com.numbergame.gamenumber.controller;

import com.numbergame.gamenumber.dto.request.BuyTurnsRequest;
import com.numbergame.gamenumber.dto.response.*;
import com.numbergame.gamenumber.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        UserInfoResponse response = userService.getUserInfo(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardResponse>>> getLeaderboard() {
        List<LeaderboardResponse> leaderboard = userService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }

    @PostMapping("/buy-turns")
    public ResponseEntity<ApiResponse<?>> buyTurns(
            @Valid @RequestBody BuyTurnsRequest request,
            Authentication authentication) {
        String username = authentication.getName();

        // Handle Stripe payment
        if ("stripe".equalsIgnoreCase(request.getPaymentMethod())) {
            if (request.getPlan() == null || request.getPlan().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Plan is required for Stripe payment"));
            }

            String checkoutUrl = userService.buyTurnsWithStripe(username, request.getPlan());
            return ResponseEntity.ok(ApiResponse.success("Stripe checkout session created",
                Map.of("checkoutUrl", checkoutUrl)));
        }

        // Handle direct payment (legacy)
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Quantity is required for direct payment"));
        }

        TransactionResponse response = userService.buyTurns(username, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Purchase successful", response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            Authentication authentication) {
        String username = authentication.getName();
        List<TransactionResponse> transactions = userService.getTransactionHistory(username);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
