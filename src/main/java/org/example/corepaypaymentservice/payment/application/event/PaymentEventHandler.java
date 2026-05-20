package org.example.corepaypaymentservice.payment.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.PaymentEventProducer;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCompletedEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentFailedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void PaymentCompletedEvent(PaymentCompletedEvent event) {
        producer.sendPaymentCompletedEvent(event);
    }

    // 2. 결제 실패 시 오더 서버로 보상 트랜잭션(주문 취소) 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void PaymentFailedEvent(PaymentFailedEvent event) {
        producer.sendPaymentFailedEvent(event);
    }

    // 3. 결제 취소 결과 오더 서버로 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void PaymentCancelEvent(PaymentRefundEvent event){
        producer.sendPaymentCancelEvent(event);
    }
}
