package org.example.corepaypaymentservice.ledger.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ledger",
        indexes = {
                @Index(name = "idx_ledger_order_id",  columnList = "orderId"),
                @Index(name = "idx_ledger_user_id",   columnList = "userId"),
                @Index(name = "idx_ledger_seller_id", columnList = "sellerId")
        }
)
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    // 판매자 지갑 연동 시 사용 (Wallet 구현 전까지 nullable)
    @Column
    private Long sellerId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerType type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 원장은 생성만 가능 — 수정/삭제 없음
    @Builder
    private Ledger(Long orderId, Long userId, Long sellerId, int amount, LedgerType type) {
        this.orderId  = orderId;
        this.userId   = userId;
        this.sellerId = sellerId;
        this.amount   = amount;
        this.type     = type;
    }

    // 정적 팩토리
    public static Ledger payment(Long orderId, Long userId, int amount) {
        return Ledger.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .type(LedgerType.PAYMENT)
                .build();
    }

    public static Ledger refund(Long orderId, Long userId, int amount) {
        return Ledger.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .type(LedgerType.REFUND)
                .build();
    }

    public static Ledger cancel(Long orderId, Long userId, int amount) {
        return Ledger.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .type(LedgerType.CANCEL)
                .build();
    }
}