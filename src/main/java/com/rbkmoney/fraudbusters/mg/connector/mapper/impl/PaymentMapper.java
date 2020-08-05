package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.domain.PaymentTool;
import com.rbkmoney.damsel.fraudbusters.PayerType;
import com.rbkmoney.damsel.fraudbusters.Payment;
import com.rbkmoney.damsel.fraudbusters.PaymentStatus;
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
public class PaymentMapper implements Mapper<InvoiceChange, MachineEvent, Payment> {

    private final HgClientService hgClientService;
    private final InfoInitializer<InvoicePaymentStatusChanged> generalInfoInitiator;

    @Override
    public boolean accept(InvoiceChange change) {
        return getChangeType().getFilter().match(change)
                && (change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus().isSetFailed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus().isSetProcessed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus().isSetCaptured());
    }

    @Override
    public Payment map(InvoiceChange change, MachineEvent event) {
        String paymentId = change.getInvoicePaymentChange().getId();
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        InvoicePaymentChangePayload payload = invoicePaymentChange.getPayload();
        InvoicePaymentStatusChanged invoicePaymentStatusChanged = payload.getInvoicePaymentStatusChanged();

        InvoicePaymentWrapper invoicePaymentWrapper = hgClientService.getInvoiceInfo(event.getSourceId(), findPayment(), paymentId, event.getEventId());

        var invoice = invoicePaymentWrapper.getInvoice();
        var invoicePayment = invoicePaymentWrapper.getInvoicePayment();

        Payer payer = invoicePayment.getPayment().getPayer();

        PaymentTool paymentTool = generalInfoInitiator.initPaymentTool(payer);
        Payment payment = new Payment()
                .setStatus(TBaseUtil.unionFieldToEnum(invoicePaymentStatusChanged.getStatus(), PaymentStatus.class))
                .setCost(invoicePayment.getPayment().getCost())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(paymentTool)
                .setId(String.join(DELIMITER, invoice.getId(), invoicePayment.getPayment().getId()))
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment))
                .setPayerType(TBaseUtil.unionFieldToEnum(payer, PayerType.class))
                .setMobile(isMobile(paymentTool))
                .setRecurrent(isRecurrent(payer))
                .setError(generalInfoInitiator.initError(invoicePaymentStatusChanged));

        log.debug("InvoicePaymentMapper payment: {}", payment);
        return payment;
    }

    private BiFunction<String, Invoice, Optional<InvoicePayment>> findPayment() {
        return (id, invoiceInfo) -> invoiceInfo.getPayments().stream()
                .filter(payment -> payment.isSetPayment() && payment.getPayment().getId().equals(id))
                .findFirst();
    }

    @Override
    public EventType getChangeType() {
        return EventType.INVOICE_PAYMENT_STATUS_CHANGED;
    }


    public boolean isRecurrent(Payer payer) {
        return payer.isSetRecurrent() || payer.isSetCustomer();
    }

    public boolean isMobile(PaymentTool paymentTool) {
        return paymentTool.isSetBankCard() && paymentTool.getBankCard().isSetTokenProvider();
    }
}
