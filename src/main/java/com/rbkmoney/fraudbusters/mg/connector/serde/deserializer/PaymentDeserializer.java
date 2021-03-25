package com.rbkmoney.fraudbusters.mg.connector.serde.deserializer;

import com.rbkmoney.damsel.fraudbusters.Payment;
import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.Map;

@Slf4j
public class PaymentDeserializer extends AbstractThriftDeserializer<Payment> {

    @Override
    public Payment deserialize(String topic, byte[] data) {
        return deserialize(data, new Payment());
    }
}
