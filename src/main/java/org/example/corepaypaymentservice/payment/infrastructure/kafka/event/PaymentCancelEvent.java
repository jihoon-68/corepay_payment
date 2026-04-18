package org.example.corepaypaymentservice.payment.infrastructure.kafka.event;

import lombok.Builder;
import org.example.corepaypaymentservice.payment.application.CancelReason;

@Builder
public record OrderCancelEvent(
        Long orderId,
        CancelReason reason
) {
}
