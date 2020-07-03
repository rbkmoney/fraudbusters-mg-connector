package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.fraudbusters.Refund;
import com.rbkmoney.damsel.fraudbusters.RefundStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.EventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.initializer.InfoInitializer;
import com.rbkmoney.fraudbusters.mg.connector.service.HgClientService;
import com.rbkmoney.geck.common.util.TBaseUtil;
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

    private final HgClientService hgClientService;
    private final InfoInitializer<InvoicePaymentRefundStatusChanged> generalInfoInitiator;

    @Override
    public Refund map(InvoiceChange change, MachineEvent event) {
        log.debug("RefundPaymentMapper change: {} event: {}", change, event);

        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();
        InvoicePaymentRefundChange invoicePaymentRefundChange = invoicePaymentChange.getPayload().getInvoicePaymentRefundChange();
        InvoicePaymentRefundChangePayload payload = invoicePaymentRefundChange.getPayload();
        InvoicePaymentRefundStatusChanged invoicePaymentRefundStatusChanged = payload.getInvoicePaymentRefundStatusChanged();
        String refundId = invoicePaymentRefundChange.getId();

        InvoicePaymentWrapper invoicePaymentWrapper = hgClientService.getInvoiceInfo(event.getSourceId(), findPayment(),
                paymentId, refundId, event.getEventId());

        var invoice = invoicePaymentWrapper.getInvoice();
        var invoicePayment = invoicePaymentWrapper.getInvoicePayment();

        Payer payer = invoicePayment.getPayment().getPayer();

        Refund refund = new Refund()
                .setStatus(TBaseUtil.unionFieldToEnum(payload.getInvoicePaymentRefundStatusChanged().getStatus(), RefundStatus.class))
                .setCost(invoicePayment.getPayment().getCost())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(generalInfoInitiator.initPaymentTool(payer))
                .setId(String.join(DELIMITER, invoice.getId(), invoicePayment.getPayment().getId(),
                        invoicePaymentRefundChange.getId()))
                .setPaymentId(String.join(DELIMITER, invoice.getId(), invoicePayment.getPayment().getId()))
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment))
                .setError(generalInfoInitiator.initError(invoicePaymentRefundStatusChanged));

        log.debug("RefundPaymentMapper refund: {}", refund);
        return refund;
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
