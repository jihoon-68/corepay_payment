package org.example.corepaypaymentservice.payment.presentation;

import lombok.RequiredArgsConstructor;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.payment.application.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {

        return ResponseEntity.ok(paymentService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getPaymentList(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(paymentService.getList());
    }
}
