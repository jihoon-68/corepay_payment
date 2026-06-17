package org.example.corepaypaymentservice.payment.application;

import org.example.corepaypaymentservice.payment.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.payment.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentDto;
import org.example.corepaypaymentservice.payment.presentation.dto.res.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse pay(ProcessPaymentCommand command);
    void cancelPayment(CancelPaymentCommand command);
    PaymentDto get(Long id);
    List<PaymentDto> getList();
    void delete(Long id);
}
