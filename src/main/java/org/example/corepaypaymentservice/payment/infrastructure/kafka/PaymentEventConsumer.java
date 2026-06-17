package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaycommon.log.KafkaMdcHelper;
import org.example.corepaypaymentservice.payment.application.PaymentService;
import org.example.corepaypaymentservice.payment.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {


    private final PaymentService paymentService;
    private final KafkaMdcHelper kafkaMdcHelper;

    // 오더 서버가 발행한 결재 취소 이벤트 수신
    @KafkaListener(topics = "payment-cancel-topic", groupId = "payment-group",concurrency = "2")
    public void consumeOrderCancelEvent(@Payload String message, @Header(value = "X-Trace-Id", required = false) String traceId){

        kafkaMdcHelper.processEventWithMdc(traceId, message, PaymentCancelEvent.class, event->{
            CancelPaymentCommand command = CancelPaymentCommand.builder()
                    .orderId(event.orderId())
                    .reason(event.reason())
                    .build();

            paymentService.cancelPayment(command);
        });
    }

}
