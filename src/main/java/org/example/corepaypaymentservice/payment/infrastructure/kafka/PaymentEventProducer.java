package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.outbox.OutboxEvent;
import org.example.corepaycommon.outbox.OutboxEventPublisher;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final OutboxEventPublisher outboxEventPublisher;

    public void publishImmediately(OutboxEvent outboxEvent) {
        if (outboxEvent == null) return;
        try {
            outboxEventPublisher.publish(outboxEvent);
        } catch (Exception e) {
            log.warn("[Outbox 즉시 발행 실패] 스케줄러가 재시도 예정. topic={}", outboxEvent.getTopic());
        }
    }
}
