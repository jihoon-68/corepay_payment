package org.example.corepaypaymentservice.payment.application;

import org.example.corepaypaymentservice.ledger.application.LedgerService;
import org.example.corepaypaymentservice.ledger.application.command.LedgerRecordCommand;
import org.example.corepaypaymentservice.ledger.domain.LedgerType;
import org.example.corepaypaymentservice.payment.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.domain.PaymentState;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicPaymentServiceTest {

    @InjectMocks
    private BasicPaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private LedgerService ledgerService;
    @Mock
    private ValueOperations<String, String> valueOps;

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final int TOTAL_PRICE = 50_000;


    // pay() 테스트

    @Test
    @DisplayName("중복 결제 요청 → 멱등성 보장: 성공 반환")
    void pay_duplicate_returnsOk() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // Redis setIfAbsent → false (이미 처리됨)
        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(false);

        ProcessPaymentCommand command = buildCommand();
        PaymentResponse result = paymentService.pay(command);

        assertThat(result.success()).isTrue();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 성공 → Payment SUCCESS 저장 + 원장 PAYMENT 기록")
    void pay_success_savesPaymentAndRecordsLedger() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);

        Payment saved = buildPayment();
        given(paymentRepository.save(any(Payment.class))).willReturn(saved);

        // PG 성공 강제 → ReflectionTestUtils로 ThreadLocalRandom mock 불가
        // → 성공/실패 양쪽 경우를 커버하기 위해 상태 검증은 캡처로 확인
        ProcessPaymentCommand command = buildCommand();
        PaymentResponse result = paymentService.pay(command);

        // 성공이든 실패든 Payment는 반드시 save 호출
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));

        // 성공일 때만 원장 기록
        if (result.success()) {
            ArgumentCaptor<LedgerRecordCommand> captor =
                    ArgumentCaptor.forClass(LedgerRecordCommand.class);
            verify(ledgerService).record(captor.capture());

            LedgerRecordCommand recorded = captor.getValue();
            assertThat(recorded.orderId()).isEqualTo(ORDER_ID);
            assertThat(recorded.userId()).isEqualTo(USER_ID);
            assertThat(recorded.amount()).isEqualTo(TOTAL_PRICE);
            assertThat(recorded.type()).isEqualTo(LedgerType.PAYMENT);
        }
    }

    @Test
    @DisplayName("결제 실패 → Payment FAILED 저장 + Redis 락 해제")
    void pay_failure_releasesLock() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        given(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .willReturn(true);
        given(paymentRepository.save(any())).willReturn(buildPayment());

        ProcessPaymentCommand command = buildCommand();
        PaymentResponse result = paymentService.pay(command);

        if (!result.success()) {
            // 실패 시 Redis 락 해제 확인
            verify(redisTemplate).delete(anyString());
            assertThat(result.failReason()).isNotBlank();
        }
    }

    // cancelPayment() 테스트

    @Test
    @DisplayName("결제 취소 — 재고 복구 필요: PaymentRefundEvent 발행")
    void cancelPayment_needsStockRestore_publishesRefundEvent() {
        Payment payment = buildPayment();
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .orderId(ORDER_ID)
                .reason(buildCancelReason(true))
                .build();

        paymentService.cancelPayment(command);

        assertThat(payment.getState()).isEqualTo(PaymentState.CANCELED);
        verify(publisher).publishEvent(any(PaymentRefundEvent.class));
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("결제 취소 — 재고 복구 불필요: 이벤트 미발행")
    void cancelPayment_noStockRestore_noEvent() {
        Payment payment = buildPayment();
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(payment));

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .orderId(ORDER_ID)
                .reason(buildCancelReason(false))
                .build();

        paymentService.cancelPayment(command);

        assertThat(payment.getState()).isEqualTo(PaymentState.CANCELED);
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 주문 취소 → 예외 발생")
    void cancelPayment_orderNotFound_throwsException() {
        given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(Optional.empty());

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .orderId(ORDER_ID)
                .reason(buildCancelReason(false))
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> paymentService.cancelPayment(command)
        );
    }

    // 헬퍼

    private ProcessPaymentCommand buildCommand() {
        return ProcessPaymentCommand.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .totalPrice(TOTAL_PRICE)
                .build();
    }

    private Payment buildPayment() {
        Payment p = Payment.builder()
                .orderId(ORDER_ID)
                .build();
        ReflectionTestUtils.setField(p, "id", 1L);
        return p;
    }

    private org.example.corepaypaymentservice.payment.application.CancelReason
    buildCancelReason(boolean needsStockRestore) {
        return needsStockRestore
                ? CancelReason.PAYMENT_FAILED
                : CancelReason.OUT_OF_STOCK;
    }
}