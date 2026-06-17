package org.example.corepaypaymentservice.ledger.infrastructure.db;

import org.example.corepaypaymentservice.ledger.domain.Ledger;
import org.example.corepaypaymentservice.ledger.domain.LedgerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findByOrderId(Long orderId);

    List<Ledger> findByUserId(Long userId);

    List<Ledger> findBySellerId(Long sellerId);

    List<Ledger> findByType(LedgerType type);
}
