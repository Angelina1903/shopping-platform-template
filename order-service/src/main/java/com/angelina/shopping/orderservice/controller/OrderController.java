package com.angelina.shopping.orderservice.controller;

import com.angelina.shopping.orderservice.entity.Order;
import com.angelina.shopping.orderservice.repo.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    // topic 名字先固定，后面 payment-service 也用同一个
    private static final String TOPIC_ORDER_CREATED = "order-created";

    public OrderController(OrderRepository repo, KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.repo = repo;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    public record CreateOrderRequest(Long accountId, String itemId, Integer quantity, Integer priceCents) {}

    public record OrderCreatedEvent(String orderId, Long accountId, String itemId, Integer totalCents) {}

    @GetMapping
    public List<Order> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid order id");
        }
        return repo.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody CreateOrderRequest req) {
        if (req.accountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId is required");
        }
        if (req.itemId() == null || req.itemId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required");
        }
        int qty = (req.quantity() == null) ? 1 : req.quantity();
        if (qty <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 1");
        }
        if (req.priceCents() == null || req.priceCents() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priceCents must be >= 0");
        }

        int total = req.priceCents() * qty;

        Order order = new Order(
                UUID.randomUUID(),
                req.accountId(),
                req.itemId(),
                qty,
                total,
                "CREATED",
                Instant.now()
        );

        // 1) save to Cassandra
        Order saved = repo.save(order);

        // 2) produce Kafka event
        try {
            String payload = objectMapper.writeValueAsString(
                    new OrderCreatedEvent(saved.getId().toString(), saved.getAccountId(), saved.getItemId(), saved.getTotalCents())
            );
            kafka.send(TOPIC_ORDER_CREATED, saved.getId().toString(), payload);
        } catch (Exception e) {
            // bare minimum：不让它把请求搞挂（你要更严格也可以返回 500）
            System.out.println("Failed to publish Kafka event: " + e.getMessage());
        }

        return saved;
    }
}