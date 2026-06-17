package org.example.corepaypaymentservice.ledger.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerTest {

    @Test
    @DisplayName("정적 팩토리 payment() — PAYMENT 타입 생성")
    void payment_factory_createsCorrectType() {
        Ledger ledger = Ledger.payment(1L, 10L, 50_000);

        assertThat(ledger.getOrderId()).isEqualTo(1L);
        assertThat(ledger.getUserId()).isEqualTo(10L);
        assertThat(ledger.getAmount()).isEqualTo(50_000);
        assertThat(ledger.getType()).isEqualTo(LedgerType.PAYMENT);
        assertThat(ledger.getSellerId()).isNull();
    }

    @Test
    @DisplayName("정적 팩토리 refund() — REFUND 타입 생성")
    void refund_factory_createsCorrectType() {
        Ledger ledger = Ledger.refund(1L, 10L, 50_000);
        assertThat(ledger.getType()).isEqualTo(LedgerType.REFUND);
    }

    @Test
    @DisplayName("정적 팩토리 cancel() — CANCEL 타입 생성")
    void cancel_factory_createsCorrectType() {
        Ledger ledger = Ledger.cancel(1L, 10L, 50_000);
        assertThat(ledger.getType()).isEqualTo(LedgerType.CANCEL);
    }
}