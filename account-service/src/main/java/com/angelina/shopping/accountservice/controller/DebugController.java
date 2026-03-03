/*package com.angelina.shopping.accountservice.controller;

import com.angelina.shopping.accountservice.client.ItemClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    private final ItemClient itemClient;

    public DebugController(ItemClient itemClient) {
        this.itemClient = itemClient;
    }

    @GetMapping("/debug/item-health")
    public String itemHealth() {
        return itemClient.health();
    }
}*/