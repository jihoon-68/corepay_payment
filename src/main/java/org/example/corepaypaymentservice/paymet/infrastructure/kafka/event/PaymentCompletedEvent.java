package org.example.corepaypaymentservice.paymet.infrastructure.kafka.event;

public record PaymentCompletedEvent(
        Long orderId,
        String status
) {
}
