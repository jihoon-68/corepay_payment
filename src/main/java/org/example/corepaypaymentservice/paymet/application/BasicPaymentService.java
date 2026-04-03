package org.example.corepaypaymentservice.paymet.application;

import lombok.RequiredArgsConstructor;
import org.example.corepaypaymentservice.paymet.infrastructure.kafka.event.PaymentCompletedEvent;
import org.example.corepaypaymentservice.paymet.presentation.dto.req.PaymentCreatReq;
import org.example.corepaypaymentservice.paymet.presentation.dto.req.PaymentUpdateStateReq;
import org.example.corepaypaymentservice.paymet.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.paymet.domain.Payment;
import org.example.corepaypaymentservice.paymet.infrastructure.kafka.producer.PaymentEventProducer;
import org.example.corepaypaymentservice.paymet.infrastructure.db.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicPaymentService implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer producer;

    @Override
    @Transactional
    public void creat(PaymentCreatReq req) {
        Payment payment = Payment.builder()
                .orderId(req.orderId())
                .build();
        paymentRepository.save(payment);
        producer.consumePaymentCompleted(new PaymentCompletedEvent(payment.getOrderId(), "COMPLETED"));
    }

    @Override
    @Transactional
    public void updateState(PaymentUpdateStateReq req) {
        Payment payment = paymentRepository.findById(req.id())
                .orElseThrow(() -> new IllegalArgumentException("결제 내역이 없습니다."));
        payment.updateState(req.state());
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto get(Long id) {
        return PaymentDto.of(paymentRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDto> getList() {
        return paymentRepository.findAll().stream()
                .map(PaymentDto::of)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        paymentRepository.deleteById(id);
    }
}
