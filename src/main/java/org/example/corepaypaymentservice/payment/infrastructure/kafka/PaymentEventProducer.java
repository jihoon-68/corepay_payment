package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 3. 결제 취소 결과 오더 서버로 발송
    public void sendPaymentCancelEvent(PaymentRefundEvent event){
        sendMessage("payment-refund-topic", event);
    }

    private void sendMessage(String topic, Object event) {
        try {
            String messagePayload = objectMapper.writeValueAsString(event);

            // 현재 스레드의 MDC에서 Trace ID 꺼내기
            String traceId = MDC.get("traceId");

            // MessageBuilder를 사용하여 페이로드(JSON)와 카프카 헤더(Trace ID)를 함께 포장
            Message<String> kafkaMessage = MessageBuilder
                    .withPayload(messagePayload)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader("X-Trace-Id", traceId != null ? traceId : "UNKNOWN-TRACE")
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.info("[카프카 발송 성공] 토픽: {}, 메시지: {}", topic, kafkaMessage);
        } catch (JsonProcessingException e) {
            log.error("카프카 메시지 직렬화 에러. 토픽: {}", topic, e);
        }
    }
}
