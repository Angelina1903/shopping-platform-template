package com.angelina.shopping.orderservice.kafka;

import com.angelina.shopping.orderservice.repo.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentProcessedListener {

    private static final String TOPIC_PAYMENT_PROCESSED = "payment-processed";

    private final OrderRepository repo;

    public PaymentProcessedListener(OrderRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = TOPIC_PAYMENT_PROCESSED, groupId = "order-service")
    public void handlePaymentProcessed(String orderId) {
        System.out.println("📩 Order service received payment-processed: " + orderId);

        UUID id = UUID.fromString(orderId);
        repo.findById(id).ifPresent(order -> {
            order.setStatus("PAID");
            repo.save(order);
            System.out.println("✅ Order updated to PAID: " + orderId);
        });
    }
}