package com.angelina.shopping.paymentservice.entity;

import java.time.Instant;
import java.util.UUID;

public class Payment {

    public enum Status {
        SUBMITTED,   // payment created
        PAID,        // processed/charged
        REFUNDED     // reversed/refund
    }

    private UUID id;
    private UUID orderId;
    private Integer amountCents;
    private Status status;
    private Instant createdAt;
    private Instant updatedAt;

    public Payment() {}

    public Payment(UUID id, UUID orderId, Integer amountCents, Status status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public Integer getAmountCents() { return amountCents; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}