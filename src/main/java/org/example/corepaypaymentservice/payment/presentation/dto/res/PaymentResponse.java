package org.example.corepaypaymentservice.payment.presentation.dto.res;

public record PaymentResponse(
        boolean success,
        String failReason
) {
    public static PaymentResponse ok() {
        return new PaymentResponse(true, null);
    }

    public static PaymentResponse fail(String reason) {
        return new PaymentResponse(false, reason);
    }
}