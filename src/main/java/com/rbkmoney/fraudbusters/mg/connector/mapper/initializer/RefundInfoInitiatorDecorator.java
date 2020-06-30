package com.rbkmoney.fraudbusters.mg.connector.mapper.initializer;

import com.rbkmoney.damsel.domain.Failure;
import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.OperationFailure;
import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.fraudbusters.ClientInfo;
import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.fraudbusters.ProviderInfo;
import com.rbkmoney.damsel.fraudbusters.ReferenceInfo;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentRefundStatusChanged;
import com.rbkmoney.geck.serializer.kit.tbase.TErrorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundInfoInitiatorDecorator implements InfoInitializer<InvoicePaymentRefundStatusChanged> {

    public static final String OPERATION_TIMEOUT = "operation_timeout";

    private final GeneralInfoInitiator generalInfoInitiator;

    @Override
    public Error initError(InvoicePaymentRefundStatusChanged refundStatusChanged) {
        Error error = new Error();
        if (refundStatusChanged.getStatus().isSetFailed()) {
            OperationFailure operationFailure = refundStatusChanged.getStatus().getFailed().getFailure();
            if (operationFailure.isSetFailure()) {
                Failure failure = operationFailure.getFailure();
                error.setErrorCode(TErrorUtil.toStringVal(failure))
                        .setErrorReason(failure.getReason());
            } else if (refundStatusChanged.getStatus().getFailed().getFailure().isSetOperationTimeout()) {
                error.setErrorCode(OPERATION_TIMEOUT);
            }
        }
        return error;
    }

    @Override
    public ClientInfo initClientInfo(Payer payer) {
        return generalInfoInitiator.initClientInfo(payer);
    }

    @Override
    public void initEmail(ClientInfo clientInfo, Payer payer) {
        generalInfoInitiator.initEmail(clientInfo, payer);
    }

    @NonNull
    @Override
    public ProviderInfo initProviderInfo(InvoicePayment invoicePayment) {
        return generalInfoInitiator.initProviderInfo(invoicePayment);
    }

    @Override
    public ReferenceInfo initReferenceInfo(Invoice invoice) {
        return generalInfoInitiator.initReferenceInfo(invoice);
    }

}