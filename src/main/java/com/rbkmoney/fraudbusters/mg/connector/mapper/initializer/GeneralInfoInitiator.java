package com.rbkmoney.fraudbusters.mg.connector.mapper.initializer;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.fraudbusters.ClientInfo;
import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.fraudbusters.*;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentStatusChanged;
import com.rbkmoney.geck.serializer.kit.tbase.TErrorUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GeneralInfoInitiator implements InfoInitializer<InvoicePaymentStatusChanged> {

    public static final String OPERATION_TIMEOUT = "operation_timeout";
    public static final String UNKNOWN = "UNKNOWN";

    public Error initError(InvoicePaymentStatusChanged invoicePaymentStatusChanged) {
        Error error = null;
        if (invoicePaymentStatusChanged.getStatus().isSetFailed()) {
            error = new Error();
            OperationFailure operationFailure = invoicePaymentStatusChanged.getStatus().getFailed().getFailure();
            if (operationFailure.isSetFailure()) {
                Failure failure = operationFailure.getFailure();
                error.setErrorCode(TErrorUtil.toStringVal(failure))
                        .setErrorReason(failure.getReason());
            } else if (invoicePaymentStatusChanged.getStatus().getFailed().getFailure().isSetOperationTimeout()) {
                error.setErrorCode(OPERATION_TIMEOUT);
            } else {
                error.setErrorCode("unknown error");
            }
        }
        return error;
    }

    public ClientInfo initClientInfo(Payer payer) {
        ClientInfo clientInfo = new ClientInfo();
        if (payer.isSetPaymentResource() && payer.getPaymentResource().isSetResource()) {
            DisposablePaymentResource resource = payer.getPaymentResource().getResource();
            if (resource.isSetClientInfo()) {
                var clientInfoRes = resource.getClientInfo();
                String ipAddress = clientInfoRes.getIpAddress();
                clientInfo.setIp(ipAddress);
                clientInfo.setFingerprint(clientInfo.getFingerprint());
            }
        }
        initEmail(clientInfo, payer);
        return clientInfo;
    }

    public void initEmail(ClientInfo clientInfo, Payer payer) {
        if (payer.isSetPaymentResource() && payer.getPaymentResource().isSetContactInfo()) {
            clientInfo.setEmail(payer.getPaymentResource().getContactInfo().getEmail());
        }
    }

    @NonNull
    public ProviderInfo initProviderInfo(InvoicePayment invoicePayment) {
        ProviderInfo providerInfo = new ProviderInfo();
        Payer payer = invoicePayment.getPayment().getPayer();
        PaymentTool paymentTool = initPaymentTool(payer);
        if (invoicePayment.isSetRoute()) {
            providerInfo.setTerminalId(String.valueOf(invoicePayment.getRoute().getTerminal().getId()));
            providerInfo.setProviderId(String.valueOf(invoicePayment.getRoute().getProvider().getId()));
        } else {
            providerInfo.setTerminalId(UNKNOWN);
            providerInfo.setProviderId(UNKNOWN);
        }
        if (paymentTool != null && paymentTool.isSetBankCard()) {
            BankCard bankCard = paymentTool.getBankCard();
            providerInfo.setCountry(bankCard.isSetIssuerCountry() ? bankCard.getIssuerCountry().name() : UNKNOWN);
        }
        return providerInfo;
    }

    public PaymentTool initPaymentTool(Payer payer) {
        PaymentTool paymentTool = null;
        if (payer.isSetPaymentResource() && payer.getPaymentResource().isSetResource()) {
            DisposablePaymentResource resource = payer.getPaymentResource().getResource();
            paymentTool = resource.getPaymentTool();
        } else if (payer.isSetCustomer()) {
            CustomerPayer customer = payer.getCustomer();
            paymentTool = customer.getPaymentTool();
        } else if (payer.isSetRecurrent()) {
            RecurrentPayer recurrent = payer.getRecurrent();
            paymentTool = recurrent.getPaymentTool();
        } else {
            log.warn("Unknown payment tool in payer: {}", payer);
        }
        return paymentTool;
    }

    public ReferenceInfo initReferenceInfo(com.rbkmoney.damsel.domain.Invoice invoice) {
        return ReferenceInfo.merchant_info(new MerchantInfo()
                .setPartyId(invoice.getOwnerId())
                .setShopId(invoice.getShopId())
        );
    }

}
