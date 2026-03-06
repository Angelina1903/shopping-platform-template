package com.angelina.shopping.orderservice.kafka;

import com.angelina.shopping.orderservice.repo.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentRefundedListener {

    private static final String TOPIC_PAYMENT_REFUNDED = "payment-refunded";

    private final OrderRepository repo;

    public PaymentRefundedListener(OrderRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = TOPIC_PAYMENT_REFUNDED, groupId = "order-service")
    public void handlePaymentRefunded(String orderId) {
        System.out.println("↩️ Order service received payment-refunded: " + orderId);

        UUID id = UUID.fromString(orderId);
        repo.findById(id).ifPresent(order -> {
            order.setStatus("REFUNDED");
            repo.save(order);
            System.out.println("✅ Order updated to REFUNDED: " + orderId);
        });
    }
}