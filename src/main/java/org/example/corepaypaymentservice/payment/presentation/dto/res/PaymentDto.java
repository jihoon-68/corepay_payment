package org.example.corepaypaymentservice.payment.presentation.dto.res;

import lombok.Builder;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.domain.PaymentState;

import java.time.LocalDateTime;

@Builder
public record PaymentDto(
        Long id,
        Long orderId,
        PaymentState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentDto from(Payment payment){
        return PaymentDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .state(payment.getState())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
