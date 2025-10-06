package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.entity.Transaction;
import com.numbergame.gamenumber.enums.SubscriptionPlan;
import com.numbergame.gamenumber.repository.TransactionRepository;
import com.numbergame.gamenumber.service.IRedisService;
import com.numbergame.gamenumber.service.IStripeService;
import com.numbergame.gamenumber.utils.GameUtils;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeServiceImpl implements IStripeService {

    private final TransactionRepository transactionRepository;
    private final IRedisService redisService;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.products.monthly.price-id}")
    private String monthlyPriceId;

    @Value("${stripe.products.quarterly.price-id}")
    private String quarterlyPriceId;

    @Value("${stripe.products.yearly.price-id}")
    private String yearlyPriceId;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe API initialized successfully");
    }

    @Override
    public String createCheckoutSession(Long userId, String username, SubscriptionPlan plan) {
        try {
            String priceId = getPriceIdForPlan(plan);

            // Create metadata to track user and plan
            Map<String, String> metadata = new HashMap<>();
            metadata.put("userId", userId.toString());
            metadata.put("username", username);
            metadata.put("plan", plan.getPlanName());
            metadata.put("turns", String.valueOf(plan.getTurns()));

            SessionCreateParams params = SessionCreateParams.builder()
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build()
                )
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/cancel")
                .putAllMetadata(metadata)
                .build();

            Session session = Session.create(params);

            log.info("Created Stripe checkout session for user {} - Plan: {}, Session ID: {}",
                username, plan.getPlanName(), session.getId());

            return session.getUrl();

        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session: {}", e.getMessage(), e);
            throw new RuntimeException("Payment initialization failed: " + e.getMessage());
        }
    }

    @Override
    public void handleSuccessfulPayment(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);

            if ("paid".equals(session.getPaymentStatus())) {
                Map<String, String> metadata = session.getMetadata();
                Long userId = Long.parseLong(metadata.get("userId"));
                String plan = metadata.get("plan");

                SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(plan);
                processPaymentSuccess(userId, subscriptionPlan, sessionId);
            }
        } catch (StripeException e) {
            log.error("Failed to retrieve Stripe session: {}", e.getMessage(), e);
            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processPaymentSuccess(Long userId, SubscriptionPlan plan, String sessionId) {
        log.info("Processing payment success for user {} - Plan: {}", userId, plan.getPlanName());

        // Add turns to user in Redis
        redisService.addTurns(userId, plan.getTurns());

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .transactionType("PURCHASE")
                .turnsAdded(plan.getTurns())
                .amount(plan.getPrice())
                .paymentMethod("STRIPE")
                .paymentStatus("COMPLETED")
                .transactionRef(GameUtils.generateTransactionRef())
                .stripeSessionId(sessionId)
                .subscriptionPlan(plan.getPlanName())
                .build();

        transactionRepository.save(transaction);

        // Invalidate user cache
        redisService.invalidateUserCache(userId);

        log.info("✅ Payment processed successfully - User: {}, Plan: {}, Turns added: {}, Amount: ${}",
            userId, plan.getPlanName(), plan.getTurns(), plan.getPrice());
    }

    private String getPriceIdForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case MONTHLY -> monthlyPriceId;
            case QUARTERLY -> quarterlyPriceId;
            case YEARLY -> yearlyPriceId;
        };
    }
}
