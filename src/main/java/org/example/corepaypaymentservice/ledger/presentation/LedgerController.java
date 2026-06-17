package org.example.corepaypaymentservice.ledger.presentation;

import lombok.RequiredArgsConstructor;
import org.example.corepaypaymentservice.ledger.application.LedgerService;
import org.example.corepaypaymentservice.ledger.presentation.dto.LedgerDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledgers")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    // 주문별 원장 조회
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<LedgerDto>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ledgerService.getByOrderId(orderId));
    }

    // 유저별 원장 조회
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<LedgerDto>> getByUser(
            @RequestHeader("X-User-Id") Long requesterId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ledgerService.getByUserId(userId));
    }
}