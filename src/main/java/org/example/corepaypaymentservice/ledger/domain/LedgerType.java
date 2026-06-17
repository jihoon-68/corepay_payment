package org.example.corepaypaymentservice.ledger.domain;

import lombok.Getter;

@Getter
public enum LedgerType {
    PAYMENT("결제"),
    REFUND("환불"),
    CANCEL("결제취소");

    private final String description;

    LedgerType(String description) {
        this.description = description;
    }
}