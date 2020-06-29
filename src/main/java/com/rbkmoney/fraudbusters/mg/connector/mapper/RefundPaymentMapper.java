package com.rbkmoney.fraudbusters.mg.connector.mapper;

import com.rbkmoney.damsel.domain.Failure;
import com.rbkmoney.damsel.domain.OperationFailure;
import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.fraudbusters.Refund;
import com.rbkmoney.damsel.fraudbusters.RefundStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.EventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.service.HgClientService;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.geck.serializer.kit.tbase.TErrorUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.BiFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundPaymentMapper implements Mapper<InvoiceChange, MachineEvent, Refund> {

    public static final String OPERATION_TIMEOUT = "operation_timeout";

    private final HgClientService hgClientService;

    @Override
    public Refund map(InvoiceChange change, MachineEvent event) {
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();
        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange.getPayload().getInvoicePaymentRefundChange();
        InvoicePaymentRefundChangePayload payload = invoicePaymentRefundChange.getPayload();
        InvoicePaymentRefundStatusChanged invoicePaymentRefundStatusChanged = payload.getInvoicePaymentRefundStatusChanged();
        String refundId = invoicePaymentRefundChange.getId();

        InvoicePaymentWrapper invoicePaymentWrapper = hgClientService.getInvoiceInfo(
                event.getSourceId(), findPayment(), paymentId, refundId, event.getEventId());

        Refund refund = new Refund();

        refund.setStatus(TBaseUtil.unionFieldToEnum(payload
                .getInvoicePaymentRefundStatusChanged()
                .getStatus(), RefundStatus.class));

        if (invoicePaymentRefundStatusChanged.getStatus().isSetFailed()) {
            OperationFailure operationFailure = invoicePaymentRefundStatusChanged.getStatus().getFailed().getFailure();
            if (operationFailure.isSetFailure()) {
                Failure failure = operationFailure.getFailure();
                refund.setError(new Error()
                        .setErrorCode(TErrorUtil.toStringVal(failure))
                        .setErrorReason(failure.getReason()));
            } else if (invoicePaymentRefundStatusChanged.getStatus().getFailed().getFailure().isSetOperationTimeout()) {
                refund.setError(new Error()
                        .setErrorCode(OPERATION_TIMEOUT));
            }
        }

        log.debug("RefundPaymentMapper refund: {}", refund);
        return new Refund();
    }

    private BiFunction<String, Invoice, Optional<InvoicePayment>> findPayment() {
        return (id, invoiceInfo) -> invoiceInfo.getPayments().stream()
                .filter(payment ->
                        payment.isSetPayment()
                                && payment.isSetRefunds()
                                && payment.getRefunds().stream()
                                .anyMatch(refund -> refund.getRefund().getId().equals(id))
                )
                .findFirst();
    }

    @Override
    public EventType getChangeType() {
        return EventType.INVOICE_PAYMENT_REFUND_STATUS_CHANGED;
    }

}
