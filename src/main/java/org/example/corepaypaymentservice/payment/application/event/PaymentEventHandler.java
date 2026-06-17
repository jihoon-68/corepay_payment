package org.example.corepaypaymentservice.payment.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.PaymentEventProducer;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentEventProducer producer;

    // 3. 결제 취소 결과 오더 서버로 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void PaymentCancelEvent(PaymentRefundEvent event){
        producer.sendPaymentCancelEvent(event);
    }
}
