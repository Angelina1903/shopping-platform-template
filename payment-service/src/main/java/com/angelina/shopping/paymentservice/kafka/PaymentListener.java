package com.angelina.shopping.paymentservice.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {

    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String TOPIC_PAYMENT_PROCESSED = "payment-processed";

    private final KafkaTemplate<String, String> kafka;

    public PaymentListener(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = TOPIC_ORDER_CREATED, groupId = "payment-service")
    public void handleOrderCreated(String message) {

        System.out.println("💰 Payment service received order event:");
        System.out.println(message);

        String orderId = message.replaceAll(".*\"orderId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        kafka.send(TOPIC_PAYMENT_PROCESSED, orderId, orderId);

        System.out.println("✅ Payment processed, sent payment-processed for orderId=" + orderId);
    }
}