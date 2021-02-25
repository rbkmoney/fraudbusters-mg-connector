package com.rbkmoney.fraudbusters.mg.connector.constant;

import com.rbkmoney.geck.filter.Condition;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;

public enum InvoiceEventType implements EventType{

    INVOICE_PAYMENT_STATUS_CHANGED("invoice_payment_change.payload.invoice_payment_status_changed", new IsNullCondition().not()),
    INVOICE_PAYMENT_REFUND_STATUS_CHANGED("invoice_payment_change.payload.invoice_payment_refund_change.payload.invoice_payment_refund_status_changed", new IsNullCondition().not()),
    INVOICE_PAYMENT_CHARGEBACK_STATUS_CHANGED("invoice_payment_change.payload.invoice_payment_chargeback_change.payload.invoice_payment_chargeback_status_changed", new IsNullCondition().not());
    Filter filter;

    InvoiceEventType(String path, Condition... conditions) {
        this.filter = new PathConditionFilter(new PathConditionRule(path, conditions));
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

}
