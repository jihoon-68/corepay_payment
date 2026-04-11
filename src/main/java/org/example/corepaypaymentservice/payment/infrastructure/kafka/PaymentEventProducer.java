package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCompletedEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentFailedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 1. 결제 최종 성공 시 오더 서버로 발송
    public void sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        sendMessage("payment-completed-topic", event);
    }

    // 2. 결제 실패 시 오더 서버로 보상 트랜잭션(주문 취소) 발송
    public void sendPaymentFailedEvent(PaymentFailedEvent event) {
        sendMessage("payment-failed-topic", event);
    }

    // 3. 결제 취소 결과 오더 서버로 발송
    public void sendPaymentCancelEvent(PaymentCancelEvent event){
        sendMessage("payment-cancel-topic", event);
    }

    private void sendMessage(String topic, Object event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, message);
            log.info("[카프카 발송 성공] 토픽: {}, 메시지: {}", topic, message);
        } catch (JsonProcessingException e) {
            log.error("카프카 메시지 직렬화 에러. 토픽: {}", topic, e);
        }
    }
}
