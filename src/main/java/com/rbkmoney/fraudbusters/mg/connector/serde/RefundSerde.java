package com.rbkmoney.fraudbusters.mg.connector.serde;


import com.rbkmoney.damsel.fraudbusters.Refund;
import com.rbkmoney.fraudbusters.mg.connector.serde.deserializer.RefundDeserializer;
import com.rbkmoney.kafka.common.serialization.ThriftSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@Slf4j
public class RefundSerde implements Serde<Refund> {


    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public void close() {

    }

    @Override
    public Serializer<Refund> serializer() {
        return new ThriftSerializer<>();
    }

    @Override
    public Deserializer<Refund> deserializer() {
        return new RefundDeserializer();
    }

}
