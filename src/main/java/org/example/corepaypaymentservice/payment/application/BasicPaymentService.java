package org.example.corepaypaymentservice.payment.application;

import lombok.RequiredArgsConstructor;
import org.example.corepaypaymentservice.payment.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.CreatedPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCompletedEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentFailedEvent;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicPaymentService implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public void creat(CreatedPaymentCommand command) {
        Payment payment = Payment.builder()
                .orderId(command.orderId())
                .build();
        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void processPayment(ProcessPaymentCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(()-> new RuntimeException("결재를 진행할 주문을 찾을 수 없습니다. OrderId: "+command.orderId()));


        try {

            // 외부 PG사 결제 API 호출 (가상)
            // boolean isSuccess = pgClient.pay(command.amount(), ...);
            boolean isSuccess = ThreadLocalRandom.current().nextBoolean(); //임시 50% 확률로 true 또는 false 반환

            // PG사 응답 결과에 따른 성공/실패 분기 처리
            if (isSuccess) {
                payment.success();
                PaymentCompletedEvent event = PaymentCompletedEvent.builder().orderId(payment.getOrderId()).build();
                publisher.publishEvent(event);
            } else {
                payment.failed();
                PaymentFailedEvent event = PaymentFailedEvent.builder()
                        .orderId(payment.getOrderId())
                        .reason("PG사 결제 승인 거절")
                        .build();

                publisher.publishEvent(event);
            }

        } catch (Exception e) {
            // PG사 서버 다운, 네트워크 에러 등의 예외 발생 시 실패 처리
            payment.failed();
            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .orderId(payment.getOrderId())
                    .reason("결제 시스템 통신 에러: " + e.getMessage())
                    .build();

            publisher.publishEvent(event);
        }
        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void cancelPayment(CancelPaymentCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(()-> new RuntimeException("환불할 주문을 찾을 수 없습니다. OrderId: "+command.orderId()));

        payment.canceled();
        PaymentCancelEvent event = PaymentCancelEvent.builder().orderId(command.orderId()).build();

        publisher.publishEvent(event);
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto get(Long id) {
        return PaymentDto.from(paymentRepository.findById(id).orElseThrow(() -> new RuntimeException("해당 결재 정보 없습니다")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> getList() {
        return paymentRepository.findAll().stream()
                .map(PaymentDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        paymentRepository.deleteById(id);
    }
}
