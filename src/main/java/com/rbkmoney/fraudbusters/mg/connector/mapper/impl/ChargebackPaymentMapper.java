package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.fraudbusters.Chargeback;
import com.rbkmoney.damsel.fraudbusters.ChargebackStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.EventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
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

    @Override
    public Chargeback map(InvoiceChange change, MachineEvent event) {
        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();
        InvoicePaymentChargebackChange invoicePaymentChargebackChange = invoicePaymentChange.getPayload().getInvoicePaymentChargebackChange();
        InvoicePaymentChargebackChangePayload payload = invoicePaymentChargebackChange.getPayload();
        InvoicePaymentChargebackStatusChanged invoicePaymentChargebackStatusChanged = payload.getInvoicePaymentChargebackStatusChanged();

        String chargebackId = invoicePaymentChargebackChange.getId();
        InvoicePaymentWrapper invoicePaymentWrapper = hgClientService.getInvoiceInfo(event.getSourceId(), findPayment(),
                paymentId, chargebackId, event.getEventId());

        Chargeback chargeback = new Chargeback();
        chargeback.setStatus(TBaseUtil.unionFieldToEnum(
                invoicePaymentChargebackStatusChanged.getStatus(), ChargebackStatus.class));

        log.debug("ChargebackPaymentMapper chargebackRow: {}", chargeback);
        return new Chargeback();
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
    public EventType getChangeType() {
        return EventType.INVOICE_PAYMENT_CHARGEBACK_STATUS_CHANGED;
    }

}
