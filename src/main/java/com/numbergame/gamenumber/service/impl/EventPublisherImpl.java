package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.config.KafkaConfig;
import com.numbergame.gamenumber.event.GameEvent;
import com.numbergame.gamenumber.service.IEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherImpl implements IEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishGameEvent(GameEvent event) {
        publishEvent(KafkaConfig.GAME_EVENTS_TOPIC, event);
    }

    @Override
    public void publishUserEvent(GameEvent event) {
        publishEvent(KafkaConfig.USER_EVENTS_TOPIC, event);
    }

    @Override
    public void publishTransactionEvent(GameEvent event) {
        publishEvent(KafkaConfig.TRANSACTION_EVENTS_TOPIC, event);
    }

    @Override
    public void publishAuditEvent(GameEvent event) {
        publishEvent(KafkaConfig.AUDIT_EVENTS_TOPIC, event);
    }

    private void publishEvent(String topic, GameEvent event) {
        try {
            // Use userId as partition key for better distribution
            String key = event.getUserId() != null ? event.getUserId().toString() : event.getEventId();

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event published to topic: {} - partition: {} - offset: {}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event to topic: {} - error: {}", topic, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing event to topic: {}", topic, e);
        }
    }
}
