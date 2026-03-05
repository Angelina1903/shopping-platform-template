package com.angelina.shopping.paymentservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void handleOrderCreated(String message) {

        System.out.println("💰 Payment service received order event:");
        System.out.println(message);

        // 这里未来可以：
        // 1. 创建 payment record
        // 2. 调用支付逻辑
        // 3. 发送 payment-processed event
    }
}