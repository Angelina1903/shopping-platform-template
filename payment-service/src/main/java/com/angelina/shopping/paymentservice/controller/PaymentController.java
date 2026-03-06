package com.angelina.shopping.paymentservice.controller;

import com.angelina.shopping.paymentservice.entity.Payment;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final String TOPIC_PAYMENT_REFUNDED = "payment-refunded";

    // ===== In-memory storage (bare minimum) =====
    // paymentId -> Payment
    private static final Map<UUID, Payment> PAYMENTS_BY_ID = new ConcurrentHashMap<>();
    // orderId -> paymentId  (idempotency key)
    private static final Map<UUID, UUID> PAYMENT_ID_BY_ORDER_ID = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, String> kafka;

    public PaymentController(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public record SubmitPaymentRequest(String orderId, Integer amountCents) {}
    public record UpdatePaymentRequest(String status) {}

    // Submit Payment
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment submit(@RequestBody SubmitPaymentRequest req) {
        UUID orderId = parseUUID(req.orderId(), "invalid orderId");
        if (req.amountCents() == null || req.amountCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amountCents must be > 0");
        }

        // ===== Idempotency: same orderId returns existing payment =====
        UUID existingPaymentId = PAYMENT_ID_BY_ORDER_ID.get(orderId);
        if (existingPaymentId != null) {
            Payment existing = PAYMENTS_BY_ID.get(existingPaymentId);
            if (existing != null) {
                return existing;
            }
        }

        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.now();
        Payment p = new Payment(
                paymentId,
                orderId,
                req.amountCents(),
                Payment.Status.SUBMITTED,
                now,
                now
        );

        PAYMENTS_BY_ID.put(paymentId, p);
        PAYMENT_ID_BY_ORDER_ID.put(orderId, paymentId);
        return p;
    }

    // Payment Lookup
    @GetMapping("/{id}")
    public Payment get(@PathVariable String id) {
        UUID paymentId = parseUUID(id, "invalid payment id");
        Payment p = PAYMENTS_BY_ID.get(paymentId);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found");
        }
        return p;
    }

    // Update Payment (e.g., manually set status)
    @PutMapping("/{id}")
    public Payment update(@PathVariable String id, @RequestBody UpdatePaymentRequest req) {
        UUID paymentId = parseUUID(id, "invalid payment id");
        Payment p = PAYMENTS_BY_ID.get(paymentId);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found");
        }

        if (req.status() == null || req.status().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        Payment.Status newStatus;
        try {
            newStatus = Payment.Status.valueOf(req.status().trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status, use SUBMITTED/PAID/REFUNDED");
        }

        p.setStatus(newStatus);
        p.setUpdatedAt(Instant.now());
        return p;
    }

    // Reverse Payment (Refund)
    @PostMapping("/{id}/refund")
    public Payment refund(@PathVariable String id) {
        UUID paymentId = parseUUID(id, "invalid payment id");
        Payment p = PAYMENTS_BY_ID.get(paymentId);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "payment not found");
        }

        if (p.getStatus() != Payment.Status.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only PAID payment can be refunded");
        }

        p.setStatus(Payment.Status.REFUNDED);
        p.setUpdatedAt(Instant.now());

        try {
            kafka.send(TOPIC_PAYMENT_REFUNDED, p.getOrderId().toString());
            System.out.println("↩️ Published payment-refunded: " + p.getOrderId());
        } catch (Exception e) {
            System.out.println("❌ Failed to publish payment-refunded: " + e.getMessage());
        }

        return p;
    }

    private static UUID parseUUID(String raw, String errMsg) {
        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
    }

    // ===== helper for Kafka listener to reuse (optional) =====
    public static Payment upsertPaidByOrderId(UUID orderId, int amountCents) {
        UUID existingPaymentId = PAYMENT_ID_BY_ORDER_ID.get(orderId);
        Instant now = Instant.now();

        if (existingPaymentId != null) {
            Payment existing = PAYMENTS_BY_ID.get(existingPaymentId);
            if (existing != null) {
                existing.setAmountCents(amountCents);
                existing.setStatus(Payment.Status.PAID);
                existing.setUpdatedAt(now);
                return existing;
            }
        }

        UUID newId = UUID.randomUUID();
        Payment created = new Payment(
                newId,
                orderId,
                amountCents,
                Payment.Status.PAID,
                now,
                now
        );

        PAYMENTS_BY_ID.put(newId, created);
        PAYMENT_ID_BY_ORDER_ID.put(orderId, newId);
        return created;
    }
}