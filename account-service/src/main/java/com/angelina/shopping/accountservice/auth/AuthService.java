package com.angelina.shopping.accountservice.auth;

import com.angelina.shopping.accountservice.entity.Account;
import com.angelina.shopping.accountservice.repo.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final AccountRepository repo;

    private final Map<String, Long> tokenToAccountId = new ConcurrentHashMap<>();

    public AuthService(AccountRepository repo) {
        this.repo = repo;
    }

    public LoginResult login(String email, String password) {
        Account a = repo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("invalid email/password"));

        if (a.getPassword() == null || !a.getPassword().equals(password)) {
            throw new IllegalArgumentException("invalid email/password");
        }

        String token = UUID.randomUUID().toString();
        tokenToAccountId.put(token, a.getId());
        return new LoginResult(token, a.getId());
    }

    public Long validate(String token) {
        return tokenToAccountId.get(token);
    }

    public record LoginResult(String token, Long accountId) {}
}