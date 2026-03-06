package com.angelina.shopping.paymentservice;

import com.angelina.shopping.paymentservice.controller.PaymentController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafka;

    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";

    public OrderCreatedConsumer(ObjectMapper objectMapper, KafkaTemplate<String, String> kafka) {
        this.objectMapper = objectMapper;
        this.kafka = kafka;
    }

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void handleOrderCreated(String message) {

        try {
            System.out.println("💰 Payment service received order event:");
            System.out.println(message);

            JsonNode node = objectMapper.readTree(message);

            String orderIdStr = node.get("orderId").asText();
            int totalCents = node.get("totalCents").asInt();

            UUID orderId = UUID.fromString(orderIdStr);

            // 创建或更新 payment（幂等）
            PaymentController.upsertPaidByOrderId(orderId, totalCents);

            // 发送 payment processed event
            kafka.send(PAYMENT_PROCESSED_TOPIC, orderIdStr, orderIdStr);

            System.out.println("✅ Payment processed for order: " + orderIdStr);

        } catch (Exception e) {
            System.out.println("❌ Failed to process order-created event: " + e.getMessage());
        }
    }
}