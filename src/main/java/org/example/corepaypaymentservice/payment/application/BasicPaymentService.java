package org.example.corepaypaymentservice.payment.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.CreatedPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCompletedEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentFailedEvent;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPaymentService implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher publisher;
    private final StringRedisTemplate redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

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
        if (isDuplicateEvent(command.orderId())) {
            log.warn("이미 처리 중이거나 완료된 결제 요청입니다. OrderId: {}", command.orderId());
            return;
        }

        Payment payment = null;

        try {
            // 2. DB 조회
            payment = paymentRepository.findByOrderId(command.orderId())
                    .orElseThrow(() -> new RuntimeException("결제를 진행할 주문을 찾을 수 없습니다. OrderId: " + command.orderId()));

            // 3. 외부 PG사 결제 API 호출 (가상)
            boolean isSuccess = ThreadLocalRandom.current().nextBoolean();

            // 4. 비즈니스 로직에 의한 성공/실패 분기
            if (isSuccess) {
                handlePaymentSuccess(payment);
            } else {
                // 💡 헬퍼 메서드에 command.orderId()를 직접 전달
                handlePaymentFailure(command.orderId(), payment, CancelReason.PAYMENT_FAILED);
            }

        } catch (Exception e) {
            // 5. 시스템 에러 발생 시 처리
            log.error("결제 처리 중 시스템 에러 발생. OrderId: {}, 사유: {}", command.orderId(), e.getMessage(), e);

            // 💡 지훈님 아이디어 적용: 널 체크와 이벤트 발행을 한 메서드에서 깔끔하게 통합 처리!
            handlePaymentFailure(command.orderId(), payment, CancelReason.PAYMENT_FAILED);

            releaseLock(command.orderId());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Override
    @Transactional
    public void cancelPayment(CancelPaymentCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(()-> new RuntimeException("환불할 주문을 찾을 수 없습니다. OrderId: "+command.orderId()));

        payment.canceled();
        if (command.reason().isNeedStockRestore()){
            PaymentRefundEvent event = PaymentRefundEvent.builder().orderId(command.orderId()).build();
            publisher.publishEvent(event);
        }
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


    private void handlePaymentSuccess(Payment payment) {
        payment.success();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(payment.getOrderId())
                .build();
        paymentRepository.save(payment);
        publisher.publishEvent(event);
        log.info("결제 성공 이벤트 발행 완료. OrderId: {}", payment.getOrderId());
    }


    private void handlePaymentFailure(Long orderId, Payment payment, CancelReason reason) {
        // 1. 엔티티가 정상적으로 조회된 상태라면 상태를 Failed로 업데이트 (NPE 완벽 방어)
        if (payment != null) {
            payment.failed();
            paymentRepository.save(payment);
        }

        // 2. 엔티티 존재 여부와 무관하게(결제가 안 만들어졌어도), 파라미터로 받은 orderId로 취소 이벤트 발행!
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId(orderId)
                .reason(reason)
                .build();
        publisher.publishEvent(event);

        log.info("결제 실패/에러 이벤트 발행 완료. OrderId: {}, 사유: {}", orderId, reason);
    }



    private boolean isDuplicateEvent(Long orderId) {
        String lockKey = "lock:payment:order:" + orderId;
        Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);

        // setIfAbsent는 키가 없어서 세팅에 성공하면 true를 반환함.
        // 따라서 성공(true)이 아니면 중복(true)이라는 뜻.
        return Boolean.FALSE.equals(isFirstRequest);
    }

    private void releaseLock(Long orderId) {
        // 결제 처리 중 예외가 발생했을 때, 카프카 재처리를 위해 락을 해제합니다.
        String lockKey = "lock:payment:order:" + orderId;
        redisTemplate.delete(lockKey);
        log.info("결제 처리 실패로 인해 Redis 락을 해제했습니다. OrderId: {}", orderId);
    }
}
