package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.domain.PaymentTool;
import com.rbkmoney.damsel.fraudbusters.PayerType;
import com.rbkmoney.damsel.fraudbusters.Payment;
import com.rbkmoney.damsel.fraudbusters.PaymentStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.InvoiceEventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.initializer.InfoInitializer;
import com.rbkmoney.fraudbusters.mg.connector.service.HgClientService;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.mamsel.TokenProviderUtil;
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
                && (change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus()
                .isSetFailed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus()
                .isSetProcessed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus()
                .isSetCaptured());
    }

    @Override
    public Payment map(InvoiceChange change, MachineEvent event) {
        String paymentId = change.getInvoicePaymentChange().getId();
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        InvoicePaymentChangePayload payload = invoicePaymentChange.getPayload();
        InvoicePaymentStatusChanged invoicePaymentStatusChanged = payload.getInvoicePaymentStatusChanged();

        InvoicePaymentWrapper invoicePaymentWrapper = invokeHgGetInvoiceInfo(change, event, paymentId);

        var invoice = invoicePaymentWrapper.getInvoice();
        var invoicePayment = invoicePaymentWrapper.getInvoicePayment();

        Payer payer = invoicePayment.getPayment().getPayer();
        PaymentTool paymentTool = generalInfoInitiator.initPaymentTool(payer);
        Payment payment = initPayment(event, invoicePaymentStatusChanged, invoice, invoicePayment, payer, paymentTool);

        log.debug("Map payment: {}", payment);
        return payment;
    }

    private Payment initPayment(MachineEvent event, InvoicePaymentStatusChanged invoicePaymentStatusChanged,
                                com.rbkmoney.damsel.domain.Invoice invoice, InvoicePayment invoicePayment, Payer payer,
                                PaymentTool paymentTool) {
        com.rbkmoney.damsel.domain.InvoicePayment payment = invoicePayment.getPayment();
        return new Payment()
                .setStatus(TBaseUtil.unionFieldToEnum(invoicePaymentStatusChanged.getStatus(), PaymentStatus.class))
                .setCost(payment.getCost())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(paymentTool)
                .setId(String.join(DELIMITER, invoice.getId(), payment.getId()))
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment))
                .setPayerType(TBaseUtil.unionFieldToEnum(payer, PayerType.class))
                .setMobile(isMobile(paymentTool))
                .setRecurrent(isRecurrent(payer))
                .setError(generalInfoInitiator.initError(invoicePaymentStatusChanged));
    }

    @Override
    public InvoiceEventType getChangeType() {
        return InvoiceEventType.INVOICE_PAYMENT_STATUS_CHANGED;
    }

    private InvoicePaymentWrapper invokeHgGetInvoiceInfo(InvoiceChange change, MachineEvent event, String paymentId) {
        try {
            return hgClientService.getInvoiceInfo(event.getSourceId(), findPayment(), paymentId, event.getEventId());
        } catch (Exception e) {
            log.warn("Problem when get invoice info for event: {} change: {} status: {}", event,
                    change, change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus());
            throw e;
        }
    }

    private BiFunction<String, Invoice, Optional<InvoicePayment>> findPayment() {
        return (id, invoiceInfo) -> invoiceInfo.getPayments().stream()
                .filter(payment -> payment.isSetPayment() && payment.getPayment().getId().equals(id))
                .findFirst();
    }

    public boolean isRecurrent(Payer payer) {
        return payer.isSetRecurrent() || payer.isSetCustomer();
    }

    public boolean isMobile(PaymentTool paymentTool) {
        return paymentTool.isSetBankCard() && TokenProviderUtil.getTokenProviderName(paymentTool.getBankCard()) != null;
    }

}
