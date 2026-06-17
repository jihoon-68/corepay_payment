CREATE TABLE ledger(
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    seller_id BIGINT,                                  -- Wallet 연동 전까지 nullable
                    amount INT NOT NULL,
                    type VARCHAR(20) NOT NULL,                   -- PAYMENT | REFUND | CANCEL
                    created_at DATETIME(6) NOT NULL,

                    CONSTRAINT pk_ledger PRIMARY KEY (id),
                    CONSTRAINT chk_ledger_amount CHECK (amount > 0),
                    CONSTRAINT chk_ledger_type   CHECK (type IN ('PAYMENT', 'REFUND', 'CANCEL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 주문별 조회
CREATE INDEX idx_ledger_order_id  ON ledger (order_id);

-- 유저별 조회
CREATE INDEX idx_ledger_user_id   ON ledger (user_id);

-- 판매자별 조회 (Wallet 연동 후 사용)
CREATE INDEX idx_ledger_seller_id ON ledger (seller_id);