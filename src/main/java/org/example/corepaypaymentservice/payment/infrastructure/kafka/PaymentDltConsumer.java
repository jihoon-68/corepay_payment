package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDltConsumer {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    /**
     * payment-cancel-topic 재시도 3회 모두 실패 시 DLQ 수신
     * → 결제 레코드를 CANCEL_FAILED 상태로 강제 마킹
     * → TODO: 슬랙/운영 알림 연동
     */
    @KafkaListener(
            topics = "payment-cancel-topic.DLT",
            groupId = "payment-group-dlt",
            containerFactory = "dltKafkaListenerContainerFactory"
    )
    public void consumePaymentCancelDlt(@Payload String message) {
        log.error("[DLQ 수신] payment-cancel 재시도 전부 실패. message={}", message);

        try {
            PaymentCancelEvent event = objectMapper.readValue(message, PaymentCancelEvent.class);

            paymentRepository.findByOrderId(event.orderId()).ifPresentOrElse(
                    payment -> {
                        payment.cancelFailed(); // Payment 도메인에 CANCEL_FAILED 상태 추가 필요
                        paymentRepository.save(payment);
                        log.warn("[DLQ 강제 마킹] orderId={} → CANCEL_FAILED", event.orderId());
                    },
                    () -> log.error("[DLQ 처리 불가] 결제 레코드 없음. orderId={}", event.orderId())
            );

            // TODO: alertService.sendAlert("[결제 취소 실패] orderId=" + event.orderId());
            log.error("[DLQ 운영 알림 필요] orderId={} 수동 확인 요망", event.orderId());

        } catch (Exception e) {
            // DLQ Consumer 자체 실패는 로그만 남기고 넘김 (무한루프 방지)
            log.error("[DLQ 처리 실패] message={}, error={}", message, e.getMessage());
        }
    }
}