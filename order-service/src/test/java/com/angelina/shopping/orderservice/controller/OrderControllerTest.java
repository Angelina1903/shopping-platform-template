package com.angelina.shopping.orderservice.controller;

import com.angelina.shopping.orderservice.entity.Order;
import com.angelina.shopping.orderservice.repo.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    private final OrderRepository repo = mock(OrderRepository.class);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = mock(RestTemplate.class);

    private final OrderController controller =
            new OrderController(repo, kafka, objectMapper, restTemplate);

    @Test
    @DisplayName("list should return all orders")
    void listSuccess() {
        Order o1 = new Order(UUID.randomUUID(), 1L, "item1", 1, 500, "CREATED", Instant.now());
        Order o2 = new Order(UUID.randomUUID(), 2L, "item2", 2, 1000, "PAID", Instant.now());

        when(repo.findAll()).thenReturn(List.of(o1, o2));

        List<Order> result = controller.list();

        assertEquals(2, result.size());
        assertEquals("item1", result.get(0).getItemId());
        assertEquals("item2", result.get(1).getItemId());
    }

    @Test
    @DisplayName("get should return order when found")
    void getSuccess() {
        UUID id = UUID.randomUUID();
        Order order = new Order(id, 1L, "item1", 2, 1000, "CREATED", Instant.now());

        when(repo.findById(id)).thenReturn(Optional.of(order));

        Order result = controller.get(id.toString());

        assertEquals(id, result.getId());
        assertEquals("item1", result.getItemId());
        assertEquals(1000, result.getTotalCents());
    }

    @Test
    @DisplayName("get should throw 400 when uuid invalid")
    void getBadRequestWhenInvalidId() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.get("not-a-uuid")
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("get should throw 404 when order not found")
    void getNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.get(id.toString())
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("create should save order successfully")
    void createSuccess() {
        OrderController.CreateOrderRequest req =
                new OrderController.CreateOrderRequest(2L, "item-1", 2, 500);

        when(restTemplate.getForObject(
                eq("http://localhost:9002/items/{id}"),
                eq(Map.class),
                eq("item-1")
        )).thenReturn(Map.of("id", "item-1", "name", "Test Item"));

        when(repo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = controller.create(req);

        assertEquals(2L, result.getAccountId());
        assertEquals("item-1", result.getItemId());
        assertEquals(2, result.getQuantity());
        assertEquals(1000, result.getTotalCents());
        assertEquals("CREATED", result.getStatus());

        verify(repo, times(1)).save(any(Order.class));
        verify(kafka, times(1)).send(eq("order-created"), anyString(), anyString());
    }

    @Test
    @DisplayName("create should throw 400 when accountId missing")
    void createBadRequestWhenAccountIdMissing() {
        OrderController.CreateOrderRequest req =
                new OrderController.CreateOrderRequest(null, "item-1", 2, 500);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("create should throw 400 when itemId missing")
    void createBadRequestWhenItemIdMissing() {
        OrderController.CreateOrderRequest req =
                new OrderController.CreateOrderRequest(2L, "", 2, 500);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("create should throw 400 when sync item lookup fails")
    void createBadRequestWhenItemLookupFails() {
        OrderController.CreateOrderRequest req =
                new OrderController.CreateOrderRequest(2L, "missing-item", 2, 500);

        when(restTemplate.getForObject(
                eq("http://localhost:9002/items/{id}"),
                eq(Map.class),
                eq("missing-item")
        )).thenThrow(new RuntimeException("404 Not Found"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("update should update order successfully")
    void updateSuccess() {
        UUID id = UUID.randomUUID();
        Order order = new Order(id, 2L, "item-1", 2, 1000, "CREATED", Instant.now());

        when(repo.findById(id)).thenReturn(Optional.of(order));
        when(repo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderController.UpdateOrderRequest req =
                new OrderController.UpdateOrderRequest("item-2", 3, 600);

        Order result = controller.update(id.toString(), req);

        assertEquals("item-2", result.getItemId());
        assertEquals(3, result.getQuantity());
        assertEquals(1800, result.getTotalCents());
    }

    @Test
    @DisplayName("update should throw 400 when order is cancelled")
    void updateBadRequestWhenCancelled() {
        UUID id = UUID.randomUUID();
        Order order = new Order(id, 2L, "item-1", 2, 1000, "CANCELLED", Instant.now());

        when(repo.findById(id)).thenReturn(Optional.of(order));

        OrderController.UpdateOrderRequest req =
                new OrderController.UpdateOrderRequest("item-2", 3, 600);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.update(id.toString(), req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("cancel should mark order as CANCELLED")
    void cancelSuccess() {
        UUID id = UUID.randomUUID();
        Order order = new Order(id, 2L, "item-1", 2, 1000, "CREATED", Instant.now());

        when(repo.findById(id)).thenReturn(Optional.of(order));
        when(repo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = controller.cancel(id.toString());

        assertEquals("CANCELLED", result.getStatus());
        verify(repo, times(1)).save(order);
    }
}
