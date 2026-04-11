package org.example.corepaypaymentservice.payment.presentation.dto.req;

public record PaymentUpdateStateReq(
        Long id,
        String state
) {
}
