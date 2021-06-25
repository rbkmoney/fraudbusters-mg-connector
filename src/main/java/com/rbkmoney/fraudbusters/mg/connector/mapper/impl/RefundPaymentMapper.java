package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.fraudbusters.PayerType;
import com.rbkmoney.damsel.fraudbusters.Refund;
import com.rbkmoney.damsel.fraudbusters.RefundStatus;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.InvoiceEventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.exception.NotFoundException;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.initializer.InfoInitializer;
import com.rbkmoney.fraudbusters.mg.connector.service.HgClientService;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.rbkmoney.fraudbusters.mg.connector.utils.PaymentMapperUtils.mapAllocationTransactionToRefund;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundPaymentMapper implements Mapper<InvoiceChange, MachineEvent, List<Refund>> {

    private final HgClientService hgClientService;
    private final InfoInitializer<InvoicePaymentRefundStatusChanged> generalInfoInitiator;

    @Override
    public boolean accept(InvoiceChange change) {
        return getChangeType().getFilter().match(change)
                && (change.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                .getPayload().getInvoicePaymentRefundStatusChanged().getStatus().isSetFailed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                .getPayload().getInvoicePaymentRefundStatusChanged().getStatus().isSetSucceeded());
    }

    @Override
    public List<Refund> map(InvoiceChange change, MachineEvent event) {
        log.debug("RefundPaymentMapper change: {} event: {}", change, event);

        InvoicePaymentChange invoicePaymentChange = change.getInvoicePaymentChange();
        String paymentId = invoicePaymentChange.getId();
        InvoicePaymentRefundChange invoicePaymentRefundChange =
                invoicePaymentChange.getPayload().getInvoicePaymentRefundChange();
        String refundId = invoicePaymentRefundChange.getId();

        InvoicePaymentWrapper invoicePaymentWrapper = hgClientService.getInvoiceInfo(
                event.getSourceId(), findPayment(), paymentId, refundId, event.getEventId()
        );
        com.rbkmoney.damsel.domain.Invoice invoice = invoicePaymentWrapper.getInvoice();
        String fullPaymentId = String.join(DELIMITER, invoice.getId(), paymentId);

        InvoicePaymentRefund invoicePaymentRefund = invoicePaymentWrapper.getInvoicePayment().getRefunds().stream()
                .filter(invRefund -> refundId.equals(invRefund.getRefund().getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("Refund with id %s for invoice %s not found!",
                        refundId, fullPaymentId)));

        return initRefunds(
                event,
                invoicePaymentWrapper,
                invoicePaymentRefund,
                invoicePaymentRefundChange.getPayload(),
                invoice,
                fullPaymentId,
                invoicePaymentRefundChange.getId()
        );
    }

    private List<Refund> initRefunds(MachineEvent event,
                                     InvoicePaymentWrapper invoicePaymentWrapper,
                                     InvoicePaymentRefund invoicePaymentRefund,
                                     InvoicePaymentRefundChangePayload payload,
                                     com.rbkmoney.damsel.domain.Invoice invoice,
                                     String fullPaymentId,
                                     String refundId) {
        var invoicePayment = invoicePaymentWrapper.getInvoicePayment();
        var paymentRefund = invoicePaymentRefund.getRefund();
        Payer payer = invoicePayment.getPayment().getPayer();
        Refund sourceRefund = new Refund()
                .setStatus(TBaseUtil.unionFieldToEnum(payload.getInvoicePaymentRefundStatusChanged().getStatus(),
                        RefundStatus.class))
                .setCost(paymentRefund.getCash())
                .setReferenceInfo(generalInfoInitiator.initReferenceInfo(invoice))
                .setPaymentTool(generalInfoInitiator.initPaymentTool(payer))
                .setId(String.join(DELIMITER, fullPaymentId, refundId))
                .setPaymentId(fullPaymentId)
                .setEventTime(event.getCreatedAt())
                .setClientInfo(generalInfoInitiator.initClientInfo(payer))
                .setProviderInfo(generalInfoInitiator.initProviderInfo(invoicePayment))
                .setPayerType(TBaseUtil.unionFieldToEnum(payer, PayerType.class))
                .setError(generalInfoInitiator.initError(payload.getInvoicePaymentRefundStatusChanged()));

        if (paymentRefund.isSetAllocation()) {
            List<Refund> refunds = paymentRefund.getAllocation().getTransactions().stream()
                    .map(allocTrx -> mapAllocationTransactionToRefund(sourceRefund, allocTrx))
                    .collect(Collectors.toList());
            refunds.add(sourceRefund);
            log.debug("RefundPaymentMapper returns refunds with allocation transactions: {}", refunds);
            return refunds;
        } else {
            log.debug("RefundPaymentMapper returns refund: {}", sourceRefund);
            return Arrays.asList(sourceRefund);
        }
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
    public InvoiceEventType getChangeType() {
        return InvoiceEventType.INVOICE_PAYMENT_REFUND_STATUS_CHANGED;
    }

}
