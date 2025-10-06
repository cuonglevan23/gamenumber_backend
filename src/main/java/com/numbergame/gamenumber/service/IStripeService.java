package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.enums.SubscriptionPlan;

public interface IStripeService {

    /**
     * Create Stripe checkout session for subscription plan
     * @param userId User ID
     * @param username Username
     * @param plan Subscription plan
     * @return Checkout session URL
     */
    String createCheckoutSession(Long userId, String username, SubscriptionPlan plan);

    /**
     * Handle successful payment webhook and add turns to user
     * @param sessionId Stripe session ID
     */
    void handleSuccessfulPayment(String sessionId);

    /**
     * Process completed payment and add turns to user
     * @param userId User ID
     * @param plan Subscription plan
     * @param sessionId Stripe session ID
     */
    void processPaymentSuccess(Long userId, SubscriptionPlan plan, String sessionId);
}
