package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.fraudbusters.Withdrawal;
import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fraudbusters.mg.connector.constant.WithdrawalEventType;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogWithdrawalMapperDecorator implements Mapper<TimestampedChange, MachineEvent, Withdrawal> {

    private final WithdrawalMapper withdrawalMapper;

    @Override
    public boolean accept(TimestampedChange change) {
        return withdrawalMapper.accept(change);
    }

    @Override
    public Withdrawal map(TimestampedChange change, MachineEvent event) {
        log.debug("Withdrawal map from change: {} event: {} ", change, event);
        Withdrawal withdrawal = withdrawalMapper.map(change, event);
        log.debug("Withdrawal map result: {}", withdrawal);
        return withdrawal;
    }

    @Override
    public WithdrawalEventType getChangeType() {
        return withdrawalMapper.getChangeType();
    }

}
