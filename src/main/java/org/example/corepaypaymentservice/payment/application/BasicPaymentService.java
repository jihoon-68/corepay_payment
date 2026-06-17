package org.example.corepaypaymentservice.payment.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.ledger.application.LedgerService;
import org.example.corepaypaymentservice.ledger.application.command.LedgerRecordCommand;
import org.example.corepaypaymentservice.ledger.domain.LedgerType;
import org.example.corepaypaymentservice.payment.application.command.*;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentRefundEvent;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentResponse;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasicPaymentService implements PaymentService {

    private final LedgerService ledgerService;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher publisher;
    private final StringRedisTemplate redisTemplate;

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    // [Feign 동기] 오더 서버가 직접 호출 → 결제 레코드 생성 + 결제 처리 통합
    @Override
    @Transactional
    public PaymentResponse pay(ProcessPaymentCommand command) {
        if (isDuplicateEvent(command.orderId())) {
            log.warn("[중복 요청 무시] 이미 처리된 결제. orderId={}", command.orderId());
            // 이미 성공 처리된 경우 성공 반환 (멱등성 보장)
            return PaymentResponse.ok();
        }

        // 1. Payment 레코드 생성 (기존 creat()를 통합)
        Payment payment = Payment.builder()
                .orderId(command.orderId())
                .userId(command.userId())
                .totalPrice(command.totalPrice())
                .build();
        paymentRepository.save(payment);

        try {
            // 2. 외부 PG사 결제 API 호출 (가상 랜덤)
            boolean isSuccess = ThreadLocalRandom.current().nextBoolean();

            if (isSuccess) {
                payment.success();
                paymentRepository.save(payment);
                ledgerRecord(command.orderId(), command.userId(),command.totalPrice() ,LedgerType.PAYMENT);
                log.info("[결제 성공] orderId={}", command.orderId());
                return PaymentResponse.ok();
            } else {
                payment.failed();
                paymentRepository.save(payment);
                // 멱등성 키 제거 → 오더 서버가 재시도할 수 있도록
                releaseLock(command.orderId());
                log.warn("[결제 실패] orderId={}", command.orderId());
                return PaymentResponse.fail("PG사 결제 거절");
            }

        } catch (Exception e) {
            payment.failed();
            paymentRepository.save(payment);
            releaseLock(command.orderId());
            log.error("[결제 시스템 오류] orderId={}, 사유={}", command.orderId(), e.getMessage(), e);
            return PaymentResponse.fail("결제 처리 중 오류 발생: " + e.getMessage());
        }
    }

    // [Kafka 수신] 결제 취소 — 기존 유지
    @Override
    @Transactional
    public void cancelPayment(CancelPaymentCommand command) {
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new RuntimeException(
                        "환불할 주문을 찾을 수 없습니다. orderId=" + command.orderId()));

        payment.canceled();
        paymentRepository.save(payment);
        ledgerRecord(command.orderId(),payment.getUserId(),payment.getTotalPrice(), LedgerType.CANCEL);

        if (command.reason().isNeedStockRestore()) {
            publisher.publishEvent(
                    PaymentRefundEvent.builder().orderId(command.orderId()).build());
        }
        log.info("[결제 취소 완료] orderId={}", command.orderId());
    }

    // 조회 / 삭제
    @Override
    @Transactional(readOnly = true)
    public PaymentDto get(Long id) {
        return PaymentDto.from(paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 결제 정보가 없습니다.")));
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

    // private 헬퍼
    private boolean isDuplicateEvent(Long orderId) {
        String lockKey = "lock:payment:order:" + orderId;
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        return Boolean.FALSE.equals(isFirst);
    }

    private void releaseLock(Long orderId) {
        redisTemplate.delete("lock:payment:order:" + orderId);
        log.info("[Redis 락 해제] orderId={}", orderId);
    }

    private void ledgerRecord(Long orderId, Long userId, int totalPrice, LedgerType type){
        ledgerService.record(LedgerRecordCommand.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(totalPrice)
                .type(type)
                .build());
    }
}