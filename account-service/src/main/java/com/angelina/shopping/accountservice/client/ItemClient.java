package com.angelina.shopping.accountservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ItemClient {

    private final RestClient restClient;

    public ItemClient(RestClient.Builder builder,
                      @Value("${services.item-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<String> getItemsByAccountId(Long accountId) {
        return restClient.get()
                .uri("/items/by-account/{accountId}", accountId)
                .retrieve()
                .body(List.class);
    }
}