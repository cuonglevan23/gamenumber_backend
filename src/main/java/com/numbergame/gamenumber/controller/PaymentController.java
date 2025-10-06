package com.numbergame.gamenumber.controller;

import com.numbergame.gamenumber.dto.response.ApiResponse;
import com.numbergame.gamenumber.service.IStripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final IStripeService stripeService;

    @GetMapping("/success")
    public ResponseEntity<ApiResponse<String>> handlePaymentSuccess(
            @RequestParam("session_id") String sessionId) {

        log.info("Payment success callback received - Session ID: {}", sessionId);

        try {
            stripeService.handleSuccessfulPayment(sessionId);
            return ResponseEntity.ok(
                ApiResponse.success("Payment successful! Turns have been added to your account.",
                    "Payment completed successfully")
            );
        } catch (Exception e) {
            log.error("Payment success handling failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Payment verification failed: " + e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<ApiResponse<String>> handlePaymentCancel() {
        log.info("Payment cancelled by user");
        return ResponseEntity.ok(
            ApiResponse.success("Payment cancelled", "You can try again anytime")
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Stripe webhook received");

        // TODO: Implement webhook verification and processing
        // This will handle events like checkout.session.completed

        return ResponseEntity.ok("Webhook processed");
    }
}

