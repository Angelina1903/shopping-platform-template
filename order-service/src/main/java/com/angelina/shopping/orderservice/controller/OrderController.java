package com.angelina.shopping.orderservice.controller;

import com.angelina.shopping.orderservice.entity.Order;
import com.angelina.shopping.orderservice.repo.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String TOPIC_ORDER_CREATED = "order-created";

    public OrderController(OrderRepository repo,
                           KafkaTemplate<String, String> kafka,
                           ObjectMapper objectMapper,
                           RestTemplate restTemplate) {
        this.repo = repo;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public record CreateOrderRequest(Long accountId, String itemId, Integer quantity, Integer priceCents) {}
    public record UpdateOrderRequest(String itemId, Integer quantity, Integer priceCents) {}
    public record OrderCreatedEvent(String orderId, Long accountId, String itemId, Integer totalCents) {}

    @GetMapping
    public List<Order> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable String id) {
        UUID uuid = parseUuidOr400(id);
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

        // ===== synchronous communication via RestTemplate =====
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = restTemplate.getForObject(
                    "http://localhost:9002/items/{id}",
                    Map.class,
                    req.itemId()
            );
            System.out.println("✅ Sync call to item-service succeeded: " + item);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "failed to fetch item from item-service: " + e.getMessage()
            );
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

        Order saved = repo.save(order);

        try {
            String payload = objectMapper.writeValueAsString(
                    new OrderCreatedEvent(
                            saved.getId().toString(),
                            saved.getAccountId(),
                            saved.getItemId(),
                            saved.getTotalCents()
                    )
            );
            kafka.send(TOPIC_ORDER_CREATED, saved.getId().toString(), payload);
        } catch (Exception e) {
            System.out.println("Failed to publish Kafka event: " + e.getMessage());
        }

        return saved;
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable String id, @RequestBody UpdateOrderRequest req) {
        UUID uuid = parseUuidOr400(id);

        Order order = repo.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order is cancelled, cannot update");
        }

        if (req.itemId() != null && !req.itemId().isBlank()) {
            order.setItemId(req.itemId());
        }

        Integer oldQty = order.getQuantity();
        Integer oldTotal = order.getTotalCents();

        if (req.quantity() != null) {
            int newQty = req.quantity();
            if (newQty <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be >= 1");
            }
            order.setQuantity(newQty);
        }

        if (req.priceCents() != null) {
            if (req.priceCents() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priceCents must be >= 0");
            }
            int qty = (order.getQuantity() == null) ? 1 : order.getQuantity();
            order.setTotalCents(req.priceCents() * qty);
        } else if (req.quantity() != null) {
            if (oldQty != null && oldQty > 0 && oldTotal != null) {
                int unit = oldTotal / oldQty;
                order.setTotalCents(unit * order.getQuantity());
            }
        }

        return repo.save(order);
    }

    @DeleteMapping("/{id}")
    public Order cancel(@PathVariable String id) {
        UUID uuid = parseUuidOr400(id);

        Order order = repo.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));

        order.setStatus("CANCELLED");
        return repo.save(order);
    }

    private UUID parseUuidOr400(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid order id");
        }
    }
}
