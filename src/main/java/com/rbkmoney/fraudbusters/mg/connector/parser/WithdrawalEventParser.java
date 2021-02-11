package com.rbkmoney.fraudbusters.mg.connector.parser;

import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fraudbusters.mg.connector.converter.BinaryConverter;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalEventParser implements EventParser<TimestampedChange> {

    private final BinaryConverter<TimestampedChange> converter;

    @Override
    public TimestampedChange parseEvent(MachineEvent message) {
        try {
            byte[] bin = message.getData().getBin();
            return converter.convert(bin, TimestampedChange.class);
        } catch (Exception e) {
            log.error("Exception when parse message e: ", e);
        }
        return null;
    }
}
