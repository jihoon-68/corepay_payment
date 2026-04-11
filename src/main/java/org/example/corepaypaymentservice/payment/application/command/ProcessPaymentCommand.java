package org.example.corepaypaymentservice.payment.application.command;

import lombok.Builder;

@Builder
public record ProcessPaymentCommand(
        Long orderId
) {
}
