package com.angelina.shopping.itemservice.controller;

import com.angelina.shopping.itemservice.entity.Item;
import com.angelina.shopping.itemservice.repo.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemControllerTest {

    private final ItemRepository repo = mock(ItemRepository.class);
    private final ItemController controller = new ItemController(repo);

    @Test
    @DisplayName("create should save item successfully")
    void createSuccess() {
        Item saved = new Item("Test Item", 5.0, 0);
        saved.setId("abc123");

        when(repo.save(any(Item.class))).thenReturn(saved);

        ItemController.CreateItemRequest req =
                new ItemController.CreateItemRequest("Test Item", 500);

        Item result = controller.create(req);

        assertEquals("abc123", result.getId());
        assertEquals("Test Item", result.getName());
        assertEquals(5.0, result.getPrice());
        assertEquals(0, result.getInventory());
    }

    @Test
    @DisplayName("create should throw 400 when name is blank")
    void createBadRequestWhenNameBlank() {
        ItemController.CreateItemRequest req =
                new ItemController.CreateItemRequest("", 500);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("create should throw 400 when priceCents is negative")
    void createBadRequestWhenPriceNegative() {
        ItemController.CreateItemRequest req =
                new ItemController.CreateItemRequest("Bad Item", -1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("getById should return item when found")
    void getByIdSuccess() {
        Item item = new Item("Mouse", 19.99, 3);
        item.setId("item001");

        when(repo.findById("item001")).thenReturn(Optional.of(item));

        Item result = controller.getById("item001");

        assertEquals("item001", result.getId());
        assertEquals("Mouse", result.getName());
        assertEquals(19.99, result.getPrice());
        assertEquals(3, result.getInventory());
    }

    @Test
    @DisplayName("getById should throw 404 when item not found")
    void getByIdNotFound() {
        when(repo.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.getById("missing")
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("list should return all items")
    void listSuccess() {
        Item item1 = new Item("A", 1.0, 0);
        item1.setId("1");
        Item item2 = new Item("B", 2.0, 5);
        item2.setId("2");

        when(repo.findAll()).thenReturn(List.of(item1, item2));

        List<Item> result = controller.list();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("2", result.get(1).getId());
    }

    @Test
    @DisplayName("delete should remove item when it exists")
    void deleteSuccess() {
        when(repo.existsById("abc123")).thenReturn(true);

        assertDoesNotThrow(() -> controller.delete("abc123"));

        verify(repo).deleteById("abc123");
    }

    @Test
    @DisplayName("delete should throw 404 when item does not exist")
    void deleteNotFound() {
        when(repo.existsById("missing")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.delete("missing")
        );

        assertEquals(404, ex.getStatusCode().value());
    }
}
