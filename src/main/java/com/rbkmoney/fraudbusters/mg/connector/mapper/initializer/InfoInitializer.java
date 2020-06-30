package com.rbkmoney.fraudbusters.mg.connector.mapper.initializer;

import com.rbkmoney.damsel.domain.BankCard;
import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.domain.PaymentTool;
import com.rbkmoney.damsel.fraudbusters.*;
import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;

public interface InfoInitializer<T> {

    Error initError(T t);

    ClientInfo initClientInfo(Payer payer);

    void initEmail(ClientInfo clientInfo, Payer payer);

    ProviderInfo initProviderInfo(InvoicePayment invoicePayment);

    ReferenceInfo initReferenceInfo(Invoice invoice);

    PaymentTool initPaymentTool(Payer payer);

}
