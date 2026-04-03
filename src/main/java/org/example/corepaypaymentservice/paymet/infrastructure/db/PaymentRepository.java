package org.example.corepaypaymentservice.paymet.infrastructure.db;

import org.example.corepaypaymentservice.paymet.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
