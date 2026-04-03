package org.example.corepaypaymentservice.paymet.presentation.dto.req;

import org.example.corepaypaymentservice.paymet.domain.PaymentState;

public record PaymentUpdateStateReq(
        Long id,
        PaymentState state
) {
}
