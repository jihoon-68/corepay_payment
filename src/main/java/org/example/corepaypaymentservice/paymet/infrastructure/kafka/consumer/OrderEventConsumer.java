package org.example.corepaypaymentservice.paymet.infrastructure.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.paymet.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepaypaymentservice.paymet.presentation.dto.req.PaymentCreatReq;
import org.example.corepaypaymentservice.paymet.application.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-created-topic")
    public void consumeOrderCreated(OrderCreatedEvent event){
        log.info("주문 이벤트 수신 결제 프로세스를 시작합니다: {}", event);
        paymentService.creat(new PaymentCreatReq(event.orderId()));
    }
}
