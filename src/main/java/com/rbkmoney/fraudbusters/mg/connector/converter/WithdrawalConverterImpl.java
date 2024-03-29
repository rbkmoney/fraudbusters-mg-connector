package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WithdrawalConverterImpl implements BinaryConverter<TimestampedChange> {

    ThreadLocal<TDeserializer> thriftDeserializerThreadLocal =
            ThreadLocal.withInitial(() -> new TDeserializer(new TBinaryProtocol.Factory()));

    @Override
    public TimestampedChange convert(byte[] bin, Class<TimestampedChange> clazz) {
        TimestampedChange event = new TimestampedChange();
        try {
            thriftDeserializerThreadLocal.get().deserialize(event, bin);
        } catch (TException e) {
            log.error("Error when convert TimestampedChange e: ", e);
        }
        return event;
    }
}
