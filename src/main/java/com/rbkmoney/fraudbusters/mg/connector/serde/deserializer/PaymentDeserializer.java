package com.rbkmoney.fraudbusters.mg.connector.serde.deserializer;

import com.rbkmoney.damsel.fraudbusters.Payment;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.Map;

@Slf4j
public class PaymentDeserializer implements Deserializer<Payment> {

    ThreadLocal<TDeserializer> thriftDeserializerThreadLocal =
            ThreadLocal.withInitial(() -> new TDeserializer(new TBinaryProtocol.Factory()));

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public Payment deserialize(String topic, byte[] data) {
        log.debug("Message, topic: {}, byteLength: {}", topic, data.length);
        Payment payment = new Payment();
        try {
            thriftDeserializerThreadLocal.get().deserialize(payment, data);
        } catch (Exception e) {
            log.error("Error when deserialize ruleTemplate data: {} ", data, e);
        }
        return payment;
    }

    @Override
    public void close() {

    }

}
