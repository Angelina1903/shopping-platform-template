package com.angelina.shopping.paymentservice.controller;

import com.angelina.shopping.paymentservice.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final PaymentController controller = new PaymentController(kafka);

    @Test
    @DisplayName("submit should create payment successfully")
    void submitSuccess() {
        String orderId = UUID.randomUUID().toString();

        PaymentController.SubmitPaymentRequest req =
                new PaymentController.SubmitPaymentRequest(orderId, 1000);

        Payment result = controller.submit(req);

        assertNotNull(result.getId());
        assertEquals(UUID.fromString(orderId), result.getOrderId());
        assertEquals(1000, result.getAmountCents());
        assertEquals(Payment.Status.SUBMITTED, result.getStatus());
    }

    @Test
    @DisplayName("submit should be idempotent for same orderId")
    void submitIdempotent() {
        String orderId = UUID.randomUUID().toString();

        PaymentController.SubmitPaymentRequest req1 =
                new PaymentController.SubmitPaymentRequest(orderId, 1000);
        PaymentController.SubmitPaymentRequest req2 =
                new PaymentController.SubmitPaymentRequest(orderId, 1000);

        Payment first = controller.submit(req1);
        Payment second = controller.submit(req2);

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getOrderId(), second.getOrderId());
    }

    @Test
    @DisplayName("submit should throw when amountCents <= 0")
    void submitBadRequestWhenAmountInvalid() {
        String orderId = UUID.randomUUID().toString();

        PaymentController.SubmitPaymentRequest req =
                new PaymentController.SubmitPaymentRequest(orderId, 0);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.submit(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("submit should throw when orderId invalid")
    void submitBadRequestWhenOrderIdInvalid() {
        PaymentController.SubmitPaymentRequest req =
                new PaymentController.SubmitPaymentRequest("not-a-uuid", 1000);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.submit(req)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("get should return payment when found")
    void getSuccess() {
        String orderId = UUID.randomUUID().toString();

        Payment created = controller.submit(
                new PaymentController.SubmitPaymentRequest(orderId, 1000)
        );

        Payment found = controller.get(created.getId().toString());

        assertEquals(created.getId(), found.getId());
        assertEquals(UUID.fromString(orderId), found.getOrderId());
        assertEquals(1000, found.getAmountCents());
    }

    @Test
    @DisplayName("get should throw when payment not found")
    void getNotFound() {
        String missingId = UUID.randomUUID().toString();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.get(missingId)
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("update should change status successfully")
    void updateSuccess() {
        String orderId = UUID.randomUUID().toString();

        Payment created = controller.submit(
                new PaymentController.SubmitPaymentRequest(orderId, 1000)
        );

        Payment updated = controller.update(
                created.getId().toString(),
                new PaymentController.UpdatePaymentRequest("PAID")
        );

        assertEquals(Payment.Status.PAID, updated.getStatus());
    }

    @Test
    @DisplayName("refund should change status to REFUNDED and publish kafka event")
    void refundSuccess() {
        String orderId = UUID.randomUUID().toString();

        Payment created = controller.submit(
                new PaymentController.SubmitPaymentRequest(orderId, 1000)
        );

        controller.update(
                created.getId().toString(),
                new PaymentController.UpdatePaymentRequest("PAID")
        );

        Payment refunded = controller.refund(created.getId().toString());

        assertEquals(Payment.Status.REFUNDED, refunded.getStatus());
        verify(kafka, times(1)).send("payment-refunded", orderId);
    }

    @Test
    @DisplayName("refund should throw when payment is not PAID")
    void refundBadRequestWhenNotPaid() {
        String orderId = UUID.randomUUID().toString();

        Payment created = controller.submit(
                new PaymentController.SubmitPaymentRequest(orderId, 1000)
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> controller.refund(created.getId().toString())
        );

        assertEquals(400, ex.getStatusCode().value());
    }
}
