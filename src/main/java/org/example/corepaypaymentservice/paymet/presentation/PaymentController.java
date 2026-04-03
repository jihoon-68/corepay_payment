package org.example.corepaypaymentservice.paymet.presentation;

import lombok.RequiredArgsConstructor;
import org.example.corepaypaymentservice.paymet.presentation.dto.req.PaymentUpdateStateReq;
import org.example.corepaypaymentservice.paymet.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.paymet.application.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmPayment(@RequestBody PaymentUpdateStateReq req) {
        paymentService.updateState(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getPaymentList() {
        return ResponseEntity.ok(paymentService.getList());
    }

}
