package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    private final TransactionProcessor processor;

    public TransactionListener(TransactionProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(topics = "${general.kafka-topic}", containerFactory = "kafkaListenerContainerFactory", groupId = "forage-midas-group")
    public void listen(Transaction tx) {
        // forward to processor (which will validate & persist)
        processor.process(tx);

        // optional debug log
        System.out.println("Received transaction amount: " + tx.getAmount());
    }
}

