package com.TransactionService.service;

import com.TransactionService.dto.LedgerEntryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // This topic must match the topic your Ledger Service is listening to
    private static final String LEDGER_REQUEST_TOPIC = "external-ledger-requests";
    
    public void sendLedgerEntryRequest(LedgerEntryRequest request) {
        log.info("Sending ledger entry request to Kafka: {}", request);
        
        // Using transactionId as the message key for partitioning
        kafkaTemplate.send(LEDGER_REQUEST_TOPIC, request.getTransactionId(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent ledger request for transaction: {}", request.getTransactionId());
                    } else {
                        log.error("Failed to send ledger request", ex);
                    }
                });

    }
}