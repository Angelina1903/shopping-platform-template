package com.angelina.shopping.orderservice.dto;

public class OrderUpdateRequest {
    private String itemId;     // optional
    private Integer quantity;  // optional

    public String getItemId() { return itemId; }
    public Integer getQuantity() { return quantity; }

    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}