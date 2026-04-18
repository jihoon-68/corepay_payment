package org.example.corepaypaymentservice.payment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.application.PaymentService;
import org.example.corepaypaymentservice.payment.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.CreatedPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.OrderCreatedEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.StockDecrementedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(topics = "order-created-topic", groupId = "payment-group")
    public void consumeOrderCreatedEvent(String message){
        try{
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            log.info("[카프카 수신] 주문 생성 확인, 결재정보 생성 진행합니다. 주문 ID: {}",event.orderId());
            CreatedPaymentCommand command = CreatedPaymentCommand.builder()
                    .orderId(event.orderId())
                    .userId(event.userId())
                    .productId(event.productId())
                    .totalPrice(event.totalPrice())
                    .amount(event.amount())
                    .build();

            paymentService.creat(command);
        } catch (JsonProcessingException e){
            log.error("오더 생성 이벤트 메시지 파싱 실패. 원본 메시지: {}", message, e);
        } catch (Exception e) {
            log.error("결재 정보 생성중 예기치 않은 시스템 에러 발생",e);
        }
    }

    // 상품 서버가 발행한 재고 차감 성공 이벤트를 수신
    @KafkaListener(topics = "stock-decremented-topic", groupId = "payment-group")
    public void consumeStockDecrementedEvent(String message) {
        try {
            // 1. 수신한 JSON 문자열을 DTO 객체로 역직렬화
            StockDecrementedEvent event = objectMapper.readValue(message, StockDecrementedEvent.class);
            log.info("[카프카 수신] 재고 차감 성공 확인. 결제를 진행합니다. 주문 ID: {}", event.orderId());

            // 2. 결제 처리 비즈니스 로직 호출
            ProcessPaymentCommand command = ProcessPaymentCommand.builder().orderId(event.orderId()).build();
            paymentService.processPayment(command);

        } catch (JsonProcessingException e) {
            log.error("재고 차감 이벤트 메시지 파싱 실패. 원본 메시지: {}", message, e);
        } catch (Exception e) {
            // 결제 시스템 장애로 인해 컨슈머가 종료되지 않도록 방어 로직 추가
            log.error("결제 처리 중 예기치 않은 시스템 에러 발생", e);
        }
    }

    // 오더 서버가 발행한 결재 취소 이벤트 수신
    @KafkaListener(topics = "payment-cancel-topic", groupId = "payment-group")
    public void consumeOrderCancelEvent(String message){
        try{
            PaymentCancelEvent event = objectMapper.readValue(message, PaymentCancelEvent.class);

            log.info("[카프카 수신] 주문 취소 확인. 결제 취소를 진행합니다. 주문 ID: {}", event.orderId());

            CancelPaymentCommand command = CancelPaymentCommand.builder()
                    .orderId(event.orderId())
                    .reason(event.reason())
                    .build();

            paymentService.cancelPayment(command);

        }catch (JsonProcessingException e) {
            log.error("주문 취소 이벤트 메시지 파싱 실패. 원본 메시지: {}", message, e);
        } catch (Exception e) {
            // 결제 시스템 장애로 인해 컨슈머가 종료되지 않도록 방어 로직 추가
            log.error("주문 취소 중 예기치 않은 시스템 에러 발생", e);
        }
    }

}
