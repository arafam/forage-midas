package com.jpmc.midascore.config;

import com.jpmc.midascore.foundation.Transaction;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration that builds a ConsumerFactory for Transaction objects.
 * It prefers embedded Kafka broker addresses (spring.embedded.kafka.brokers) if present
 * (the tests will provide embedded broker addresses), otherwise falls back to standard
 * spring.kafka.bootstrap-servers if configured.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.embedded.kafka.brokers:}")
    private String embeddedBrokers;

    @Value("${spring.kafka.bootstrap-servers:}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Transaction> transactionConsumerFactory() {
        JsonDeserializer<Transaction> deserializer = new JsonDeserializer<>(Transaction.class);
        deserializer.addTrustedPackages("*"); // allow deserialization for the test Transaction class

        Map<String, Object> props = new HashMap<>();

        // Prefer embedded brokers provided by the test harness
        String brokers = (embeddedBrokers != null && !embeddedBrokers.isBlank()) ? embeddedBrokers : bootstrapServers;
        if (brokers != null && !brokers.isBlank()) {
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        }

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "midas-core-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Transaction> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Transaction> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionConsumerFactory());
        return factory;
    }
}
