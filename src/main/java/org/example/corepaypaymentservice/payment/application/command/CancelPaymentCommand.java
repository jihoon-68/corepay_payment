package org.example.corepaypaymentservice.payment.application.command;

import lombok.Builder;
import org.example.corepaypaymentservice.payment.application.CancelReason;

@Builder
public record CancelPaymentCommand(
        Long orderId,
        CancelReason reason
) {
}
