package com.rbkmoney.fraudbusters.mg.connector.serde.deserializer;

import com.rbkmoney.damsel.fraudbusters.Withdrawal;
import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WithdrawalDeserializer extends AbstractThriftDeserializer<Withdrawal> {

    @Override
    public Withdrawal deserialize(String topic, byte[] data) {
        return deserialize(data, new Withdrawal());
    }
}
