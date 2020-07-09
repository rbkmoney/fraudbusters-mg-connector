package com.rbkmoney.fraudbusters.mg.connector;

import com.rbkmoney.damsel.domain.Payer;
import com.rbkmoney.damsel.fraudbusters.PayerType;

public class PayerTypeResolver {

    public static PayerType resolve(Payer payer) {
        if (payer != null) {
            if (payer.isSetCustomer()) {
                return PayerType.customer;
            } else if (payer.isSetRecurrent()) {
                return PayerType.recurrent;
            } else if ((payer.isSetPaymentResource())) {
                return PayerType.payment_resource;
            }
        }
        return null;
    }

}
