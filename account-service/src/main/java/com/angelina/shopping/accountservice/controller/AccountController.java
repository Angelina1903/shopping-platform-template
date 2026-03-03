package com.angelina.shopping.accountservice.controller;

import com.angelina.shopping.accountservice.client.ItemClient;
import com.angelina.shopping.accountservice.entity.Account;
import com.angelina.shopping.accountservice.repo.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository repo;
    private final ItemClient itemClient;

    public AccountController(AccountRepository repo, ItemClient itemClient) {
        this.repo = repo;
        this.itemClient = itemClient;
    }

    public record CreateAccountRequest(String email, String displayName) {}

    @GetMapping
    public List<Account> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Account getById(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@RequestBody CreateAccountRequest req) {
        if (req.email() == null || req.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (req.displayName() == null || req.displayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName is required");
        }

        // 需要你 repo 里有 findByEmail
        if (repo.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        return repo.save(new Account(req.email(), req.displayName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found");
        }
        repo.deleteById(id);
    }

    @GetMapping("/{id}/items")
    public List<String> getItemsByAccount(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found");
        }
        return itemClient.getItemsByAccountId(id);
    }
}