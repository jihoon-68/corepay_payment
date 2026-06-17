package org.example.corepaypaymentservice.payment.presentation.dto.req;

public record PaymentRequest(
        Long orderId,
        Long userId,
        int totalPrice
) {}