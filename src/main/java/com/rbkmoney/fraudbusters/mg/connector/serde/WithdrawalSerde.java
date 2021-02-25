package com.rbkmoney.fraudbusters.mg.connector.serde;


import com.rbkmoney.damsel.fraudbusters.Withdrawal;
import com.rbkmoney.fraudbusters.mg.connector.serde.deserializer.WithdrawalDeserializer;
import com.rbkmoney.kafka.common.serialization.ThriftSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@Slf4j
public class WithdrawalSerde implements Serde<Withdrawal> {


    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public void close() {

    }

    @Override
    public Serializer<Withdrawal> serializer() {
        return new ThriftSerializer<>();
    }

    @Override
    public Deserializer<Withdrawal> deserializer() {
        return new WithdrawalDeserializer();
    }

}
