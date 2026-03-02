package com.angelina.shopping.itemservice.controller;

import com.angelina.shopping.itemservice.entity.Item;
import com.angelina.shopping.itemservice.repo.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository repo;

    public ItemController(ItemRepository repo) {
        this.repo = repo;
    }

    public record CreateItemRequest(String name, Integer priceCents) {}

    @GetMapping
    public List<Item> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Item getById(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Item create(@RequestBody CreateItemRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (req.priceCents() == null || req.priceCents() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priceCents must be >= 0");
        }
        return repo.save(new Item(req.name(), req.priceCents()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found");
        }
        repo.deleteById(id);
    }
}