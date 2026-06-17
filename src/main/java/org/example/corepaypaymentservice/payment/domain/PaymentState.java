package org.example.corepaypaymentservice.payment.domain;

public enum PaymentState {
    READY,
    SUCCESS,
    FAILED,
    CANCELED,
    CANCELED_FAILED;
}
