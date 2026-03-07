package com.angelina.shopping.accountservice.controller;

import com.angelina.shopping.accountservice.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public record LoginRequest(String email, String password) {}
    public record LoginResponse(String token, Long accountId) {}
    public record ValidateResponse(Long accountId) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        try {
            AuthService.LoginResult r = auth.login(req.email(), req.password());
            return new LoginResponse(r.token(), r.accountId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ValidateResponse validate(@RequestHeader(name = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        Long accountId = auth.validate(token);
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid token");
        }
        return new ValidateResponse(accountId);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}