package org.example.corepaypaymentservice.paymet.application;

import org.example.corepaypaymentservice.paymet.application.command.CancelPaymentCommand;
import org.example.corepaypaymentservice.paymet.application.command.CreatedPaymentCommand;
import org.example.corepaypaymentservice.paymet.application.command.ProcessPaymentCommand;
import org.example.corepaypaymentservice.paymet.presentation.dto.res.PaymentDto;

import java.util.List;

public interface PaymentService {
    void creat(CreatedPaymentCommand command);
    void processPayment(ProcessPaymentCommand command);
    void cancelPayment(CancelPaymentCommand command);
    PaymentDto get(Long id);
    List<PaymentDto> getList();
    void delete(Long id);
}
