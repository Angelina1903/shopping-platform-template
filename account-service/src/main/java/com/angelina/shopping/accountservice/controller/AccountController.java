package com.angelina.shopping.accountservice.controller;

import com.angelina.shopping.accountservice.client.ItemClient;
import com.angelina.shopping.accountservice.entity.Account;
import com.angelina.shopping.accountservice.exception.AccountNotFoundException;
import com.angelina.shopping.accountservice.exception.EmailAlreadyExistsException;
import com.angelina.shopping.accountservice.repo.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    public record CreateAccountRequest(
            String email,
            String displayName,
            String password,
            String shippingAddress,
            String billingAddress,
            String paymentMethod
    ) {}

    public record UpdateAccountRequest(
            String email,
            String displayName,
            String password,
            String shippingAddress,
            String billingAddress,
            String paymentMethod
    ) {}

    @GetMapping
    public List<Account> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Account getById(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@RequestBody CreateAccountRequest req) {
        if (req.email() == null || req.email().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (req.displayName() == null || req.displayName().isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        if (repo.findByEmail(req.email()).isPresent()) {
            throw new EmailAlreadyExistsException(req.email());
        }

        Account a = new Account(req.email(), req.displayName(), req.password());
        a.setShippingAddress(req.shippingAddress());
        a.setBillingAddress(req.billingAddress());
        a.setPaymentMethod(req.paymentMethod());

        return repo.save(a);
    }

    @PutMapping("/{id}")
    public Account update(@PathVariable Long id, @RequestBody UpdateAccountRequest req) {
        Account account = repo.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        if (req.email() != null && !req.email().isBlank()) {
            repo.findByEmail(req.email()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new EmailAlreadyExistsException(req.email());
                }
            });
            account.setEmail(req.email());
        }

        if (req.displayName() != null && !req.displayName().isBlank()) {
            account.setDisplayName(req.displayName());
        }

        if (req.password() != null && !req.password().isBlank()) {
            account.setPassword(req.password());
        }

        if (req.shippingAddress() != null) {
            account.setShippingAddress(req.shippingAddress());
        }

        if (req.billingAddress() != null) {
            account.setBillingAddress(req.billingAddress());
        }

        if (req.paymentMethod() != null) {
            account.setPaymentMethod(req.paymentMethod());
        }

        return repo.save(account);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new AccountNotFoundException(id);
        }
        repo.deleteById(id);
    }

    @GetMapping("/{id}/items")
    public List<String> getItemsByAccount(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new AccountNotFoundException(id);
        }
        return itemClient.getItemsByAccountId(id);
    }
}
