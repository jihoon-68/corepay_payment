package org.example.corepaypaymentservice.paymet.presentation.dto.req;

public record PaymentUpdateStateReq(
        Long id,
        String state
) {
}
