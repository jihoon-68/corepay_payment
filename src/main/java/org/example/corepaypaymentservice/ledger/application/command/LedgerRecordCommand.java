package org.example.corepaypaymentservice.ledger.application.command;

import lombok.Builder;
import org.example.corepaypaymentservice.ledger.domain.LedgerType;

@Builder
public record LedgerRecordCommand(
        Long orderId,
        Long userId,
        Long sellerId,   // nullable — Wallet 연동 전까지 null 허용
        int amount,
        LedgerType type
) {}