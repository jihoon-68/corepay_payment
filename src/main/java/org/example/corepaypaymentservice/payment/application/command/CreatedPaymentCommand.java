package org.example.corepaypaymentservice.payment.application.command;

import lombok.Builder;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.OrderItemDto;

import java.util.List;

@Builder
public record CreatedPaymentCommand(
        Long orderId,
        Long userId,
        Integer totalPrice,
        List<OrderItemDto> items
) {
}
