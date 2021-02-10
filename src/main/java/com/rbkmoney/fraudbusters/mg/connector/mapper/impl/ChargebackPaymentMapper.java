package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.fraudbusters.Chargeback;
import com.rbkmoney.damsel.fraudbusters.ChargebackCategory;
import com.rbkmoney.damsel.fraudbusters.ChargebackStatus;
import com.rbkmoney.damsel.fraudbusters.PayerType;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.InvoiceEventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.initializer.GeneralInfoInitiator;
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
public class ChargebackPaymentMapper implements Mapper<InvoiceChange, MachineEvent, Chargeback> {

    private final HgClientService hgClientService;
    private final GeneralInfoInitiator generalInfoInitiator;

    @Override
    public boolean accept(InvoiceChange change) {
        return getChangeType().getFilter().match(change)
                && (change.getInvoicePaymentChange().getPayload().getInvoicePaymentChargebackChange()
                .getPayload().getInvoicePaymentChargebackStatusChanged().getStatus().isSetRejected()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentChargebackChange()
                .getPayload().getInvoicePaymentChargebackStatusChanged().getStatus().isSetAccepted()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentChargebackChange()
                .getPayload().getInvoicePaymentChargebackStatusChanged().getStatus().isSetCancelled());
    }

    @Override
    public Chargeback map(InvoiceChange change, MachineEvent event) {
        log.debug("ChargebackPaymentMapper change: {} event: {}", change, event);

        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();
        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload().getInvoicePaymentChargebackChange();
        InvoicePaymentChargebackChangePayload payload = invoicePaymentChargebackChange.getPayload();
        InvoicePaymentChargebackStatusChanged invoicePaymentChargebackStatusChanged = payload.getInvoicePaymentChargebackStatusChanged();

        String chargebackId = invoicePaymentChargebackChange.getId();
        InvoicePaymentWrapper invoicePaymentWrapper = hgClientService.getInvoiceInfo(event.getSourceId(), findPayment(),
                paymentId, chargebackId, event.getEventId());

        var invoice = invoicePaymentWrapper.getInvoice();
        var invoicePayment = invoicePaymentWrapper.getInvoicePayment();

        Payer payer = invoicePayment.getPayment().getPayer();

        Chargeback chargeback = new Chargeback()
                .setStatus(TBaseUtil.unionFieldToEnum(invoicePaymentChargebackStatusChanged.getStatus(), ChargebackStatus.class))
                .setCost(invoicePayment.getPayment().getCost())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(generalInfoInitiator.initPaymentTool(payer))
                .setId(String.join(DELIMITER, invoice.getId(), invoicePayment.getPayment().getId(),
                        invoicePaymentChargebackChange.getId()))
                .setPaymentId(String.join(DELIMITER, invoice.getId(), invoicePayment.getPayment().getId()))
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setPayerType(TBaseUtil.unionFieldToEnum(payer, PayerType.class))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment));

        invoicePayment.getChargebacks().stream()
                .filter(chargebackVal -> chargebackVal.getChargeback().getId().equals(chargebackId))
                .findFirst()
                .ifPresent(paymentChargeback -> {
                    com.rbkmoney.damsel.domain.InvoicePaymentChargeback invoicePaymentChargeback = paymentChargeback.getChargeback();
                    chargeback.setChargebackCode(invoicePaymentChargeback.getReason().getCode() != null ?
                            invoicePaymentChargeback.getReason().getCode() : GeneralInfoInitiator.UNKNOWN)
                            .setCategory(TBaseUtil.unionFieldToEnum(invoicePaymentChargeback.getReason().getCategory(), ChargebackCategory.class));
                });

        log.debug("ChargebackPaymentMapper chargebackRow: {}", chargeback);
        return chargeback;
    }

    private BiFunction<String, Invoice, Optional<InvoicePayment>> findPayment() {
        return (id, invoiceInfo) -> invoiceInfo.getPayments().stream()
                .filter(payment -> payment.isSetPayment()
                        && payment.isSetChargebacks()
                        && payment.getChargebacks().stream()
                        .anyMatch(chargeback -> chargeback.getChargeback().getId().equals(id))
                )
                .findFirst();
    }

    @Override
    public InvoiceEventType getChangeType() {
        return InvoiceEventType.INVOICE_PAYMENT_CHARGEBACK_STATUS_CHANGED;
    }

}
