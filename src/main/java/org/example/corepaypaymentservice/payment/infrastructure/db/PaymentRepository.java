package org.example.corepaypaymentservice.payment.infrastructure.db;

import org.example.corepaypaymentservice.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
}
