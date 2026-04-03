package org.example.corepaypaymentservice.paymet.application;

import org.example.corepaypaymentservice.paymet.presentation.dto.req.PaymentCreatReq;
import org.example.corepaypaymentservice.paymet.presentation.dto.req.PaymentUpdateStateReq;
import org.example.corepaypaymentservice.paymet.presentation.dto.res.PaymentDto;

import java.util.List;

public interface PaymentService {
    void creat(PaymentCreatReq req);
    void updateState(PaymentUpdateStateReq req);
    PaymentDto get(Long id);
    List<PaymentDto> getList();
    void delete(Long id);
}
