package org.example.corepaypaymentservice.payment.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record PaymentFailedEvent(
        Long orderId,
        String reason
) {
}
