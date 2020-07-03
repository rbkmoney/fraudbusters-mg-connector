package com.rbkmoney.fraudbusters.mg.connector.domain;

import com.rbkmoney.damsel.domain.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import lombok.Data;

@Data
public class InvoicePaymentWrapper {

    private Invoice invoice;
    private InvoicePayment invoicePayment;

}
