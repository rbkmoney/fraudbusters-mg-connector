package com.rbkmoney.fraudbusters.mg.connector.serde.deserializer;

import com.rbkmoney.damsel.fraudbusters.Refund;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.Map;

@Slf4j
public class RefundDeserializer implements Deserializer<Refund> {

    ThreadLocal<TDeserializer> tDeserializerThreadLocal = ThreadLocal.withInitial(() -> new TDeserializer(new TBinaryProtocol.Factory()));

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public Refund deserialize(String topic, byte[] data) {
        log.debug("Message, topic: {}, byteLength: {}", topic, data.length);
        Refund refund = new Refund();
        try {
            tDeserializerThreadLocal.get().deserialize(refund, data);
        } catch (Exception e) {
            log.error("Error when deserialize ruleTemplate data: {} ", data, e);
        }
        return refund;
    }

    @Override
    public void close() {

    }

}
