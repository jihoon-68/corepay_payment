package org.example.corepaypaymentservice.ledger.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.ledger.application.command.LedgerRecordCommand;
import org.example.corepaypaymentservice.ledger.domain.Ledger;
import org.example.corepaypaymentservice.ledger.infrastructure.db.LedgerRepository;
import org.example.corepaypaymentservice.ledger.presentation.dto.LedgerDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicLedgerService implements LedgerService {

    private final LedgerRepository ledgerRepository;

    // REQUIRES_NEW: 결제 트랜잭션 롤백돼도 원장은 반드시 기록
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LedgerRecordCommand command) {
        Ledger ledger = Ledger.builder()
                .orderId(command.orderId())
                .userId(command.userId())
                .sellerId(command.sellerId())
                .amount(command.amount())
                .type(command.type())
                .build();

        ledgerRepository.save(ledger);
        log.info("[원장 기록] orderId={}, type={}, amount={}",
                command.orderId(), command.type(), command.amount());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerDto> getByOrderId(Long orderId) {
        return ledgerRepository.findByOrderId(orderId)
                .stream().map(LedgerDto::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerDto> getByUserId(Long userId) {
        return ledgerRepository.findByUserId(userId)
                .stream().map(LedgerDto::from).toList();
    }
}