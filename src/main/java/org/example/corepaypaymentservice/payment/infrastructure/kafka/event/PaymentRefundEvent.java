package org.example.corepaypaymentservice.payment.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record PaymentCancelEvent(
        Long orderId,
        String reason
) {
}
