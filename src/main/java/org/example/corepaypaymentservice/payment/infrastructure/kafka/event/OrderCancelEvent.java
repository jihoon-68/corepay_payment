package org.example.corepaypaymentservice.payment.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record OrderCancelEvent(
        Long orderId,
        String reason
) {
}
