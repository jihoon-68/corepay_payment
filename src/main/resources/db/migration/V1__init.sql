-- 4. 결제 (Payment) 테이블
CREATE TABLE payment (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_id BIGINT NOT NULL UNIQUE,-- FK 제약조건 없이 논리적 관계만 유지
                        user_id BIGINT NOT NULL,
                        total_price INT NOT NULL ,
                        state ENUM('READY', 'SUCCESS', 'FAILED', 'CANCELED','CANCELED_FAILED') NOT NULL DEFAULT 'READY',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 특정 주문에 대한 결제 내역을 빠르게 찾기 위한 인덱스 추가
CREATE INDEX idx_payment_order_id ON payment(order_id);