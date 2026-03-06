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

    // token -> accountId（最小实现：内存存储；重启会丢，项目够用）
    private final Map<String, Long> tokenToAccountId = new ConcurrentHashMap<>();

    public AuthService(AccountRepository repo) {
        this.repo = repo;
    }

    public LoginResult login(String email, String password) {
        Account a = repo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("invalid email/password"));

        // ✅ 最小实现：明文对比（生产环境绝对要 hash）
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