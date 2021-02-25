package com.rbkmoney.fraudbusters.mg.connector.constant;

import com.rbkmoney.geck.filter.Condition;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;

public enum WithdrawalEventType implements EventType {

    WITHDRAWAL_PAYMENT_CHARGEBACK_STATUS_CHANGED("change.status_changed.status", new IsNullCondition().not());

    Filter filter;

    WithdrawalEventType(String path, Condition... conditions) {
        this.filter = new PathConditionFilter(new PathConditionRule(path, conditions));
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

}
