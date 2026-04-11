package org.example.corepaypaymentservice.payment.application.command;

import lombok.Builder;

@Builder
public record CancelPaymentCommand(
        Long orderId
) {
}
