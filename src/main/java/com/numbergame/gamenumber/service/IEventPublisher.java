package com.numbergame.gamenumber.service;

import com.numbergame.gamenumber.event.GameEvent;

/**
 * Service for publishing events to Kafka
 */
public interface IEventPublisher {

    /**
     * Publish game event
     * @param event Game event
     */
    void publishGameEvent(GameEvent event);

    /**
     * Publish user event
     * @param event User event
     */
    void publishUserEvent(GameEvent event);

    /**
     * Publish transaction event
     * @param event Transaction event
     */
    void publishTransactionEvent(GameEvent event);

    /**
     * Publish audit event
     * @param event Audit event
     */
    void publishAuditEvent(GameEvent event);
}

