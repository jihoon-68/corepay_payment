package org.example.corepaypaymentservice.paymet.infrastructure.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.paymet.infrastructure.kafka.event.PaymentCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void consumePaymentCompleted(PaymentCompletedEvent event){
        log.info("결제 완료 이벤트 발행: {}", event);
        kafkaTemplate.send("payment-completed-topic", event);
    }
}
