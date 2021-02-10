package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.fraudbusters.Withdrawal;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentStatusChanged;
import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fraudbusters.mg.connector.constant.InvoiceEventType;
import com.rbkmoney.fraudbusters.mg.connector.constant.WithdrawalEventType;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.initializer.InfoInitializer;
import com.rbkmoney.fraudbusters.mg.connector.service.HgClientService;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalMapper implements Mapper<TimestampedChange, MachineEvent, Withdrawal> {

    private final HgClientService hgClientService;
    private final InfoInitializer<InvoicePaymentStatusChanged> generalInfoInitiator;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetStatusChanged()
                && change.getChange().getStatusChanged().isSetStatus()
                && change.getChange().getStatusChanged().getStatus().isSetFailed()
                && change.getChange().getStatusChanged().getStatus().isSetSucceeded();
    }

    @Override
    public Withdrawal map(TimestampedChange change, MachineEvent event) {
        log.debug("Withdrawal map from change: {} event: {} ", change, event);
        Withdrawal withdrawal = new Withdrawal();

        log.debug("InvoicePaymentMapper withdrawal: {}", withdrawal);
        return withdrawal;
    }

    @Override
    public WithdrawalEventType getChangeType() {
        return WithdrawalEventType.WITHDRAWAL_PAYMENT_CHARGEBACK_STATUS_CHANGED;
    }

}
