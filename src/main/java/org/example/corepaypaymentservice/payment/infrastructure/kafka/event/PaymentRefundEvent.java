package org.example.corepaypaymentservice.payment.infrastructure.kafka.event;

import lombok.Builder;

@Builder
public record PaymentRefundEvent(
        Long orderId,
        String reason
) {
}
