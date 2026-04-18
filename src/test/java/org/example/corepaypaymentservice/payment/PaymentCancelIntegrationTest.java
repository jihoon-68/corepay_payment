package org.example.corepaypaymentservice.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepaypaymentservice.payment.application.CancelReason;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.domain.PaymentState;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.PaymentCancelEvent;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.StockDecrementedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, ports = {9092})
public class PaymentCancelIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    private CountDownLatch latch;
    private String receivedMessage;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        latch = new CountDownLatch(1);
        receivedMessage = null;

        // 💡 안정성을 위한 카프카 컨슈머 준비 대기 로직 (성공 테스트와 동일)
        for (MessageListenerContainer container : kafkaListenerEndpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    // 💡 가짜 컨슈머: 이번에는 오더 서버의 '주문 취소(보상)' 토픽을 구독합니다.
    // 주의: 성공 테스트와 groupId를 다르게 주는 것이 충돌 방지에 좋습니다.
    @KafkaListener(topics = "payment-refund-topic", groupId = "test-cancel-group")
    public void listenFailMessage(String message) {
        this.receivedMessage = message;
        this.latch.countDown();
    }

    @Test
    @DisplayName("고객 요청 결제 취소시, 결제 상태를 CANCELED로 변경하고 오더 서버로 결재 취소 이벤트를 발행한다.")
    void paymentIntegrationFlow_Fail() throws Exception {
        // Given: DB에 결제 대기 상태 데이터 세팅
        Long orderId = 100L;
        Payment pendingPayment = Payment.builder()
                .orderId(orderId)
                .build();
        paymentRepository.save(pendingPayment);

        // Given: 상품 서버에서 던질 메시지 준비 (입구는 똑같음)
        PaymentCancelEvent event = new PaymentCancelEvent(orderId, CancelReason.CUSTOMER_CANCEL);
        String message = objectMapper.writeValueAsString(event);

        // When: 결제 서버로 메시지 발송 (결제 로직 시작)
        kafkaTemplate.send("payment-cancel-topic", message);

        // 비동기 통신 대기: 실패 로직이 돌아가고 취소 메시지가 튀어나올 때까지 대기
        boolean messageReceived = latch.await(1, TimeUnit.SECONDS);

        // Then 1: 카프카 통신 검증 (오더 서버로 보상 트랜잭션 메시지가 나갔는가?)
        assertThat(messageReceived).isTrue(); // 5초 안에 order-cancel-topic 으로 응답이 와야 함
        assertThat(receivedMessage).contains(String.valueOf(orderId));

        // Then 2: DB 검증 (결제 상태가 FAILED 혹은 CANCELED 로 변경되었는가?)
        Payment updatedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(updatedPayment.getState()).isEqualTo(PaymentState.CANCELED);
    }
}