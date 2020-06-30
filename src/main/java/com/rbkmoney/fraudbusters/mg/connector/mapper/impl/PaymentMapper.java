package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.domain.BankCard;
import com.rbkmoney.damsel.domain.PaymentTool;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.fraudbusters.ClientInfo;
import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.fraudbusters.*;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.fraudbusters.mg.connector.constant.EventType;
import com.rbkmoney.fraudbusters.mg.connector.domain.InvoicePaymentWrapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
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
public class PaymentMapper implements Mapper<InvoiceChange, MachineEvent, Payment> {

    public static final String OPERATION_TIMEOUT = "operation_timeout";
    public static final String UNKNOWN = "UNKNOWN";

    private final HgClientService hgClientService;

    @Override
    public boolean accept(InvoiceChange change) {
        return getChangeType().getFilter().match(change)
                && (change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus().isSetFailed()
                || change.getInvoicePaymentChange().getPayload().getInvoicePaymentStatusChanged().getStatus().isSetCancelled()
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

        Payment payment = new Payment()
                .setStatus(TBaseUtil.unionFieldToEnum(invoicePaymentStatusChanged.getStatus(), PaymentStatus.class))
                .setCost(invoicePayment.getPayment().getCost())
                .setReferenceInfo(initReferenceInfo(invoice));
        payment.setPaymentTool(com.rbkmoney.damsel.fraudbusters.PaymentTool.bank_card(new com.rbkmoney.damsel.fraudbusters.BankCard()));
        payment.setId(invoice.getId() + invoicePayment.getPayment().getId());
        payment.setEventTime(event.getCreatedAt());
        ClientInfo clientInfo = new ClientInfo();
        Payer payer = invoicePayment.getPayment().getPayer();
        ProviderInfo providerInfo = null;
        if (payer.isSetPaymentResource() && payer.getPaymentResource().isSetResource()) {
            DisposablePaymentResource resource = payer.getPaymentResource().getResource();
            var paymentTool = resource.getPaymentTool();
            providerInfo = initProviderInfo(invoicePayment, paymentTool);
            if (resource.isSetClientInfo()) {
                var clientInfoRes = resource.getClientInfo();
                String ipAddress = clientInfoRes.getIpAddress();
                clientInfo.setIp(ipAddress);
                clientInfo.setFingerprint(clientInfo.getFingerprint());
            }
            initEmail(clientInfo, payer);
        } else if (payer.isSetCustomer()) {
            CustomerPayer customer = payer.getCustomer();
            com.rbkmoney.damsel.domain.PaymentTool paymentTool = customer.getPaymentTool();
            providerInfo = initProviderInfo(invoicePayment, paymentTool);
            initEmail(clientInfo, payer);
        } else if (payer.isSetRecurrent()) {
            RecurrentPayer recurrent = payer.getRecurrent();
            PaymentTool paymentTool = recurrent.getPaymentTool();
            providerInfo = initProviderInfo(invoicePayment, paymentTool);
            initEmail(clientInfo, payer);
        } else {
            log.warn("Unknown payment tool in payer: {}", payer);
            providerInfo = new ProviderInfo();
        }

        payment.setClientInfo(clientInfo);
        payment.setProviderInfo(providerInfo);

        if (invoicePaymentStatusChanged.getStatus().isSetFailed()) {
            OperationFailure operationFailure = invoicePaymentStatusChanged.getStatus().getFailed().getFailure();
            if (operationFailure.isSetFailure()) {
                Failure failure = operationFailure.getFailure();
                payment.setError(new Error()
                        .setErrorCode(TErrorUtil.toStringVal(failure))
                        .setErrorReason(failure.getReason()));
            } else if (invoicePaymentStatusChanged.getStatus().getFailed().getFailure().isSetOperationTimeout()) {
                payment.setError(new Error()
                        .setErrorCode(OPERATION_TIMEOUT));
            }
        }

        log.debug("InvoicePaymentMapper payment: {}", payment);
        return payment;
    }

    private void initEmail(ClientInfo clientInfo, Payer payer) {
        if (payer.getPaymentResource().isSetContactInfo()) {
            clientInfo.setEmail(payer.getPaymentResource().getContactInfo().getEmail());
        }
    }

    private ProviderInfo initProviderInfo(InvoicePayment invoicePayment, PaymentTool paymentTool) {
        ProviderInfo providerInfo = new ProviderInfo();
        if (invoicePayment.isSetRoute()) {
            providerInfo.setTerminalId(String.valueOf(invoicePayment.getRoute().getTerminal().getId()));
            providerInfo.setProviderId(String.valueOf(invoicePayment.getRoute().getProvider().getId()));
        }
        if (paymentTool.isSetBankCard()) {
            BankCard bankCard = paymentTool.getBankCard();
            providerInfo.setCountry(bankCard.isSetIssuerCountry() ? bankCard.getIssuerCountry().name() : UNKNOWN);
        }
        return providerInfo;
    }

    private ReferenceInfo initReferenceInfo(com.rbkmoney.damsel.domain.Invoice invoice) {
        return ReferenceInfo.merchant_info(new MerchantInfo()
                .setPartyId(invoice.getOwnerId())
                .setShopId(invoice.getShopId())
        );
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

}
