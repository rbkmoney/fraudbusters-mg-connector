package com.rbkmoney.fraudbusters.mg.connector.mapper.initializer;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.fraudbusters.ClientInfo;
import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.fraudbusters.ProviderInfo;
import com.rbkmoney.damsel.fraudbusters.ReferenceInfo;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;

public interface InfoInitializer<T> {

    Error initError(T t);

    ClientInfo initClientInfo(Payer payer);

    void initEmail(ClientInfo clientInfo, Payer payer);

    ProviderInfo initProviderInfo(InvoicePayment invoicePayment);

    ReferenceInfo initReferenceInfo(Invoice invoice);

}
