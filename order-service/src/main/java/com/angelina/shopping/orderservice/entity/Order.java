package com.angelina.shopping.orderservice.entity;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("orders")
public class Order {

    @PrimaryKey
    private UUID id;

    private Long accountId;
    private String itemId;
    private Integer quantity;
    private Integer totalCents;
    private String status; // CREATED / PAID / CANCELLED
    private Instant createdAt;

    public Order() {}

    public Order(UUID id, Long accountId, String itemId, Integer quantity, Integer totalCents, String status, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.totalCents = totalCents;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getTotalCents() { return totalCents; }
    public void setTotalCents(Integer totalCents) { this.totalCents = totalCents; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}