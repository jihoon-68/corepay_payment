package org.example.corepaypaymentservice.paymet.infrastructure.kafka.event;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Integer totalPrice
) {
}
