package org.example.corepaypaymentservice.payment.application.command;

import lombok.Builder;

@Builder
public record CreatedPaymentCommand(
        Long orderId,
        Long userId,
        Long productId,
        Integer totalPrice,
        Integer amount
) {
}
