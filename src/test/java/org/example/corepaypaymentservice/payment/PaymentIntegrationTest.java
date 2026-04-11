package org.example.corepaypaymentservice.payment;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corepaypaymentservice.payment.domain.Payment;
import org.example.corepaypaymentservice.payment.domain.PaymentState;
import org.example.corepaypaymentservice.payment.infrastructure.db.PaymentRepository;
import org.example.corepaypaymentservice.payment.infrastructure.kafka.event.StockDecrementedEvent;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
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
public class PaymentIntegrationTest {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    // 카프카 메시지 수신을 기다리기 위한 장치 (스레드 대기용)
    private CountDownLatch latch;
    private String receivedMessage;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        // 1개의 메시지를 기다리겠다고 초기화
        latch = new CountDownLatch(1);
        receivedMessage = null;

        // 테스트 시작 전, 모든 컨슈머가 카프카 파티션에 완벽하게 할당될 때까지 대기합니다.
        for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry.getListenerContainers()) {
            // partitions 파라미터에는 @EmbeddedKafka에 설정한 파티션 개수(1)를 넣어줍니다.
            ContainerTestUtils.waitForAssignment(messageListenerContainer, 1);
        }
    }


    // 서비스로직 카프카 발송 검증을 위한 더미 컨슈머
    @KafkaListener(topics = "payment-completed-topic", groupId = "test-group")
    public void Success_listenTestMessage(String message) {
        this.receivedMessage = message;
        this.latch.countDown(); // 메시지를 받으면 대기 상태를 풀어줍니다.
    }

    @KafkaListener(topics = "payment-faile-topic", groupId = "test-group")
    public void Failed_listenTestMessage(String message) {
        this.receivedMessage = message;
        this.latch.countDown(); // 메시지를 받으면 대기 상태를 풀어줍니다.
    }


    @Test
    @DisplayName("상품 서버에서 재고 차감 성공 이벤트가 오면, 결제를 완료하고 오더 서버로 이벤트를 발행한다.")
    void paymentIntegrationFlow_Success() throws Exception {
        // Given: DB에 결제 대기 상태 데이터 세팅
        Long orderId = 100L;
        Payment pendingPayment = Payment.builder()
                .orderId(orderId)
                .build();
        paymentRepository.save(pendingPayment);

        // Given: 상품 서버가 던질 메시지 준비
        StockDecrementedEvent event = new StockDecrementedEvent(orderId);
        String message = objectMapper.writeValueAsString(event);

        // When: 실제 카프카 토픽으로 메시지 발송 (결제 서버 동작 시작)
        kafkaTemplate.send("stock-decremented-topic", message);

        // 비동기 통신이므로, 우리 가짜 컨슈머가 메시지를 받을 때까지 최대 1초간 기다림
        boolean messageReceived = latch.await(1, TimeUnit.SECONDS);

        // Then 1: DB 검증 (결제 상태가 SUCCESS로 변경되었는가?)
        Payment updatedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(updatedPayment.getState()).isEqualTo(PaymentState.SUCCESS);

        // Then 2: 카프카 통신 검증 (오더 서버용 토픽으로 완료 메시지가 발행되었는가?)
        assertThat(messageReceived).isTrue(); // 5초 안에 메시지를 받았는지 확인
        assertThat(receivedMessage).contains(String.valueOf(orderId)); // 메시지 내용에 orderId가 있는지 확인
        
    }
    @Test
    @DisplayName("상품 서버에서 재고 차감 성공 이벤트가 오면, 결제를 완료하고 오더 서버로 이벤트를 발행한다.")
    void paymentIntegrationFlow_Failed() throws Exception {
        // Given: DB에 결제 대기 상태 데이터 세팅
        Long orderId = 100L;
        Payment pendingPayment = Payment.builder()
                .orderId(orderId)
                .build();
        paymentRepository.save(pendingPayment);

        // Given: 상품 서버가 던질 메시지 준비
        StockDecrementedEvent event = new StockDecrementedEvent(orderId);
        String message = objectMapper.writeValueAsString(event);

        // When: 실제 카프카 토픽으로 메시지 발송 (결제 서버 동작 시작)
        kafkaTemplate.send("stock-decremented-topic", message);

        // 비동기 통신이므로, 우리 가짜 컨슈머가 메시지를 받을 때까지 최대 1초간 기다림
        boolean messageReceived = latch.await(1, TimeUnit.SECONDS);

        // Then 1: DB 검증 (결제 상태가 SUCCESS로 변경되었는가?)
        Payment updatedPayment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(updatedPayment.getState()).isEqualTo(PaymentState.FAILED);

        // Then 2: 카프카 통신 검증 (오더 서버용 토픽으로 완료 메시지가 발행되었는가?)
        assertThat(messageReceived).isTrue(); // 5초 안에 메시지를 받았는지 확인
        assertThat(receivedMessage).contains(String.valueOf(orderId)); // 메시지 내용에 orderId가 있는지 확인

    }

}
