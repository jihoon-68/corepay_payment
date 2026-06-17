package org.example.corepaypaymentservice.ledger.application;

import org.example.corepaypaymentservice.ledger.application.command.LedgerRecordCommand;
import org.example.corepaypaymentservice.ledger.presentation.dto.LedgerDto;

import java.util.List;

public interface LedgerService {

    void record(LedgerRecordCommand command);

    List<LedgerDto> getByOrderId(Long orderId);

    List<LedgerDto> getByUserId(Long userId);
}