package org.example.corepaypaymentservice.payment.infrastructure.kafka.event;

public record OrderItemDto(
        Long productId,
        Integer amount
) {}