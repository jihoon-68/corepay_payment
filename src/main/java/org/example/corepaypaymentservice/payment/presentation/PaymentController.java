package org.example.corepaypaymentservice.payment.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.corepaypaymentservice.payment.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.payment.presentation.dto.req.PaymentRequest;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.payment.application.PaymentService;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request) {
        ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                .orderId(request.orderId())
                .userId(request.userId())
                .totalPrice(request.totalPrice())
                .build();

        PaymentResponse result = paymentService.pay(command);

        return ResponseEntity.ok(result);
    }

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
