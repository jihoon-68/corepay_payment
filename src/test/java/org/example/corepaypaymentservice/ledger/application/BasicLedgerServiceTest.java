package org.example.corepaypaymentservice.ledger.application;

import org.example.corepaypaymentservice.ledger.application.command.LedgerRecordCommand;
import org.example.corepaypaymentservice.ledger.domain.Ledger;
import org.example.corepaypaymentservice.ledger.domain.LedgerType;
import org.example.corepaypaymentservice.ledger.infrastructure.db.LedgerRepository;
import org.example.corepaypaymentservice.ledger.presentation.dto.LedgerDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BasicLedgerServiceTest {

    @InjectMocks private BasicLedgerService ledgerService;
    @Mock private LedgerRepository ledgerRepository;

    @Test
    @DisplayName("원장 기록 — PAYMENT 타입으로 저장")
    void record_payment_savesCorrectly() {
        LedgerRecordCommand command = LedgerRecordCommand.builder()
                .orderId(1L)
                .userId(10L)
                .amount(50_000)
                .type(LedgerType.PAYMENT)
                .build();

        given(ledgerRepository.save(any(Ledger.class))).willAnswer(i -> i.getArgument(0));

        ledgerService.record(command);

        ArgumentCaptor<Ledger> captor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository).save(captor.capture());

        Ledger saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getAmount()).isEqualTo(50_000);
        assertThat(saved.getType()).isEqualTo(LedgerType.PAYMENT);
        assertThat(saved.getSellerId()).isNull(); // Wallet 연동 전 null
    }

    @Test
    @DisplayName("원장 기록 — REFUND 타입으로 저장")
    void record_refund_savesCorrectly() {
        LedgerRecordCommand command = LedgerRecordCommand.builder()
                .orderId(1L)
                .userId(10L)
                .amount(50_000)
                .type(LedgerType.REFUND)
                .build();

        given(ledgerRepository.save(any())).willAnswer(i -> i.getArgument(0));
        ledgerService.record(command);

        ArgumentCaptor<Ledger> captor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(LedgerType.REFUND);
    }

    @Test
    @DisplayName("주문별 원장 조회 — 결과 반환")
    void getByOrderId_returnsLedgerList() {
        Ledger ledger = buildLedger(1L, 10L, 50_000, LedgerType.PAYMENT);
        given(ledgerRepository.findByOrderId(1L)).willReturn(List.of(ledger));

        List<LedgerDto> result = ledgerService.getByOrderId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().orderId()).isEqualTo(1L);
        assertThat(result.getFirst().type()).isEqualTo(LedgerType.PAYMENT);
    }

    @Test
    @DisplayName("유저별 원장 조회 — 복수 결과 반환")
    void getByUserId_returnsAllLedgers() {
        given(ledgerRepository.findByUserId(10L)).willReturn(List.of(
                buildLedger(1L, 10L, 50_000, LedgerType.PAYMENT),
                buildLedger(2L, 10L, 50_000, LedgerType.REFUND)
        ));

        List<LedgerDto> result = ledgerService.getByUserId(10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LedgerDto::type)
                .containsExactly(LedgerType.PAYMENT, LedgerType.REFUND);
    }

    @Test
    @DisplayName("조회 결과 없음 → 빈 리스트 반환")
    void getByOrderId_noResult_returnsEmpty() {
        given(ledgerRepository.findByOrderId(999L)).willReturn(List.of());

        List<LedgerDto> result = ledgerService.getByOrderId(999L);

        assertThat(result).isEmpty();
    }

    // 헬퍼
    private Ledger buildLedger(Long orderId, Long userId, int amount, LedgerType type) {
        Ledger l = Ledger.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .type(type)
                .build();
        ReflectionTestUtils.setField(l, "id", orderId);
        ReflectionTestUtils.setField(l, "createdAt", LocalDateTime.now());
        return l;
    }
}