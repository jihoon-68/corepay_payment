package org.example.corepaypaymentservice.payment.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.outbox.OutboxEvent;
import org.example.corepaycommon.outbox.OutboxRepository;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.PaymentEventProducer;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final PaymentEventProducer paymentEventProducer;

    // 3. 결제 취소 결과 오더 서버로 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void PaymentCancelEvent(PaymentRefundEvent event){
        log.info("[이벤트 수신] PaymentRefundEvent orderId={}",event.orderId());
        OutboxEvent outboxEvent = saveOutbox("payment-refund-topic",event);
        paymentEventProducer.publishImmediately(outboxEvent);
    }

    private OutboxEvent saveOutbox(String topic, Object event){
        try {
            String messagePayload = objectMapper.writeValueAsString(event);

            String traceId = MDC.get("traceId");

            OutboxEvent outboxEvent = outboxRepository.save(OutboxEvent.builder()
                    .topic(topic)
                    .payload(messagePayload)
                    .traceId(traceId != null ? traceId : "UNKNOWN-TRACE")
                    .build()
            );
            log.info("[Outbox 저장 완료] 토픽: {}", topic);
            return outboxEvent;
        }catch (JsonProcessingException e){
            log.error("Outbox 메시지 직렬화 에러. 토픽: {}", topic, e);
            return null;
        }
    }
}
