package org.example.corepaypaymentservice.ledger.presentation.dto;

import org.example.corepaypaymentservice.ledger.domain.Ledger;
import org.example.corepaypaymentservice.ledger.domain.LedgerType;

import java.time.LocalDateTime;

public record LedgerDto(
        Long id,
        Long orderId,
        Long userId,
        Long sellerId,
        int amount,
        LedgerType type,
        LocalDateTime createdAt
) {
    public static LedgerDto from(Ledger ledger) {
        return new LedgerDto(
                ledger.getId(),
                ledger.getOrderId(),
                ledger.getUserId(),
                ledger.getSellerId(),
                ledger.getAmount(),
                ledger.getType(),
                ledger.getCreatedAt()
        );
    }
}