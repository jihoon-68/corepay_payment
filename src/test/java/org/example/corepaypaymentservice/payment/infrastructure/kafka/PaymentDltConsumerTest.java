package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepaypaymentservice.payment.application.CancelReason;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentDltConsumerTest {

    @InjectMocks private PaymentDltConsumer dltConsumer;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ObjectMapper objectMapper;

    @Test
    @DisplayName("DLQ 수신 — Payment 존재: CANCEL_FAILED 상태로 강제 마킹")
    void consumeDlt_paymentExists_markedAsCancelFailed() throws Exception {
        PaymentCancelEvent event = PaymentCancelEvent.builder()
                .orderId(1L)
                .reason(CancelReason.PAYMENT_FAILED)
                .build();

        given(objectMapper.readValue(anyString(), eq(PaymentCancelEvent.class)))
                .willReturn(event);

        Payment payment = Payment.builder().orderId(1L).build();
        ReflectionTestUtils.setField(payment, "id", 1L);
        given(paymentRepository.findByOrderId(1L)).willReturn(Optional.of(payment));

        dltConsumer.consumePaymentCancelDlt("{}");

        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("DLQ 수신 — Payment 없음: 로그만 남기고 예외 없음")
    void consumeDlt_paymentNotFound_noException() throws Exception {
        PaymentCancelEvent event = PaymentCancelEvent.builder()
                .orderId(999L)
                .reason(CancelReason.PAYMENT_FAILED)
                .build();

        given(objectMapper.readValue(anyString(), eq(PaymentCancelEvent.class)))
                .willReturn(event);
        given(paymentRepository.findByOrderId(999L)).willReturn(Optional.empty());

        // 예외 없이 정상 종료 확인
        dltConsumer.consumePaymentCancelDlt("{}");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("DLQ 수신 — 역직렬화 실패: 예외 없이 로그만")
    void consumeDlt_deserializationFails_noException() throws Exception {
        given(objectMapper.readValue(anyString(), eq(PaymentCancelEvent.class)))
                .willThrow(new RuntimeException("역직렬화 실패"));

        // 무한루프 방지 — 예외 삼킴 확인
        dltConsumer.consumePaymentCancelDlt("invalid-json");

        verify(paymentRepository, never()).save(any());
    }
}