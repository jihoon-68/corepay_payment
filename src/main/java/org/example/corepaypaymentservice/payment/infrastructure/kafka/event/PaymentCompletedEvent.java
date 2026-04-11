package org.example.corepaypaymentservice.paymet.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record PaymentCompletedEvent(
        Long orderId
) {
}
