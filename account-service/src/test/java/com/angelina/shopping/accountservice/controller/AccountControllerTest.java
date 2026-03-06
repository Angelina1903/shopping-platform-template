package com.angelina.shopping.accountservice.controller;

import com.angelina.shopping.accountservice.client.ItemClient;
import com.angelina.shopping.accountservice.entity.Account;
import com.angelina.shopping.accountservice.exception.AccountNotFoundException;
import com.angelina.shopping.accountservice.exception.EmailAlreadyExistsException;
import com.angelina.shopping.accountservice.repo.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    private final AccountRepository repo = mock(AccountRepository.class);
    private final ItemClient itemClient = mock(ItemClient.class);
    private final AccountController controller = new AccountController(repo, itemClient);

    @Test
    @DisplayName("create should save account successfully")
    void createSuccess() {
        Account saved = new Account("test@test.com", "Angelina", "123456");
        saved.setShippingAddress("Irvine CA");
        saved.setBillingAddress("Irvine CA");
        saved.setPaymentMethod("VISA");

        when(repo.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(repo.save(any(Account.class))).thenReturn(saved);

        AccountController.CreateAccountRequest req =
                new AccountController.CreateAccountRequest(
                        "test@test.com",
                        "Angelina",
                        "123456",
                        "Irvine CA",
                        "Irvine CA",
                        "VISA"
                );

        Account result = controller.create(req);

        assertEquals("test@test.com", result.getEmail());
        assertEquals("Angelina", result.getDisplayName());
        assertEquals("123456", result.getPassword());
        assertEquals("Irvine CA", result.getShippingAddress());
        assertEquals("Irvine CA", result.getBillingAddress());
        assertEquals("VISA", result.getPaymentMethod());
    }

    @Test
    @DisplayName("create should throw when email is blank")
    void createFailWhenEmailBlank() {
        AccountController.CreateAccountRequest req =
                new AccountController.CreateAccountRequest(
                        "",
                        "Angelina",
                        "123456",
                        "Irvine CA",
                        "Irvine CA",
                        "VISA"
                );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.create(req)
        );

        assertEquals("email is required", ex.getMessage());
    }

    @Test
    @DisplayName("create should throw when displayName is blank")
    void createFailWhenDisplayNameBlank() {
        AccountController.CreateAccountRequest req =
                new AccountController.CreateAccountRequest(
                        "test@test.com",
                        "",
                        "123456",
                        "Irvine CA",
                        "Irvine CA",
                        "VISA"
                );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.create(req)
        );

        assertEquals("displayName is required", ex.getMessage());
    }

    @Test
    @DisplayName("create should throw when password is blank")
    void createFailWhenPasswordBlank() {
        AccountController.CreateAccountRequest req =
                new AccountController.CreateAccountRequest(
                        "test@test.com",
                        "Angelina",
                        "",
                        "Irvine CA",
                        "Irvine CA",
                        "VISA"
                );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.create(req)
        );

        assertEquals("password is required", ex.getMessage());
    }

    @Test
    @DisplayName("create should throw when email already exists")
    void createFailWhenEmailExists() {
        Account existing = new Account("test@test.com", "Someone", "pw");

        when(repo.findByEmail("test@test.com")).thenReturn(Optional.of(existing));

        AccountController.CreateAccountRequest req =
                new AccountController.CreateAccountRequest(
                        "test@test.com",
                        "Angelina",
                        "123456",
                        "Irvine CA",
                        "Irvine CA",
                        "VISA"
                );

        assertThrows(EmailAlreadyExistsException.class, () -> controller.create(req));
    }

    @Test
    @DisplayName("list should return all accounts")
    void listSuccess() {
        Account a1 = new Account("a@test.com", "A", "pw1");
        Account a2 = new Account("b@test.com", "B", "pw2");

        when(repo.findAll()).thenReturn(List.of(a1, a2));

        List<Account> result = controller.list();

        assertEquals(2, result.size());
        assertEquals("a@test.com", result.get(0).getEmail());
        assertEquals("b@test.com", result.get(1).getEmail());

    }

    @Test
    @DisplayName("getById should return account when found")
    void getByIdSuccess() {
        Account account = new Account("test@test.com", "Angelina", "123456");

        when(repo.findById(1L)).thenReturn(Optional.of(account));

        Account result = controller.getById(1L);

        assertEquals("test@test.com", result.getEmail());
        assertEquals("Angelina", result.getDisplayName());
    }

    @Test
    @DisplayName("getById should throw when account not found")
    void getByIdNotFound() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> controller.getById(999L));
    }

    @Test
    @DisplayName("delete should remove account when it exists")
    void deleteSuccess() {
        when(repo.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> controller.delete(1L));

        verify(repo).deleteById(1L);
    }

    @Test
    @DisplayName("delete should throw when account does not exist")
    void deleteNotFound() {
        when(repo.existsById(999L)).thenReturn(false);

        assertThrows(AccountNotFoundException.class, () -> controller.delete(999L));
    }

    @Test
    @DisplayName("getItemsByAccount should return item list when account exists")
    void getItemsByAccountSuccess() {
        when(repo.existsById(1L)).thenReturn(true);
        when(itemClient.getItemsByAccountId(1L))
                .thenReturn(List.of("item-A-for-1", "item-B-for-1"));

        List<String> result = controller.getItemsByAccount(1L);

        assertEquals(2, result.size());
        assertEquals("item-A-for-1", result.get(0));
        assertEquals("item-B-for-1", result.get(1));
    }

    @Test
    @DisplayName("getItemsByAccount should throw when account does not exist")
    void getItemsByAccountNotFound() {
        when(repo.existsById(999L)).thenReturn(false);

        assertThrows(AccountNotFoundException.class, () -> controller.getItemsByAccount(999L));
    }
}
