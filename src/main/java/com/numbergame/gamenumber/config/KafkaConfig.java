package com.numbergame.gamenumber.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration with topic setup and partitioning strategy
 */
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id:gamenumber-consumer-group}")
    private String consumerGroupId;
    
    // Topic names
    public static final String GAME_EVENTS_TOPIC = "game-events";
    public static final String USER_EVENTS_TOPIC = "user-events";
    public static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    public static final String AUDIT_EVENTS_TOPIC = "audit-events";
    
    /**
     * Kafka Admin Configuration
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }
    
    /**
     * Topic: game-events
     * Partitions: 3 (để distribute load cho game play events)
     * Replication: 1 (single broker setup)
     */
    @Bean
    public NewTopic gameEventsTopic() {
        return TopicBuilder.name(GAME_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    /**
     * Topic: user-events
     * Partitions: 2 (authentication, registration events)
     */
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(USER_EVENTS_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }
    
    /**
     * Topic: transaction-events
     * Partitions: 2 (purchase events)
     */
    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(TRANSACTION_EVENTS_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }
    
    /**
     * Topic: audit-events
     * Partitions: 4 (high volume audit logs)
     */
    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(AUDIT_EVENTS_TOPIC)
                .partitions(4)
                .replicas(1)
                .build();
    }
    
    /**
     * Producer Factory
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    /**
     * Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.numbergame.gamenumber.*");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        return factory;
    }
}

