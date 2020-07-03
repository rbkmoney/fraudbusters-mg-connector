package com.rbkmoney.fraudbusters.mg.connector.serde.deserializer;

import com.rbkmoney.damsel.fraudbusters.Chargeback;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.Map;

@Slf4j
public class ChargebackDeserializer implements Deserializer<Chargeback> {

    ThreadLocal<TDeserializer> tDeserializerThreadLocal = ThreadLocal.withInitial(() -> new TDeserializer(new TBinaryProtocol.Factory()));

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public Chargeback deserialize(String topic, byte[] data) {
        log.debug("Message, topic: {}, byteLength: {}", topic, data.length);
        Chargeback chargeback = new Chargeback();
        try {
            tDeserializerThreadLocal.get().deserialize(chargeback, data);
        } catch (Exception e) {
            log.error("Error when deserialize ruleTemplate data: {} ", data, e);
        }
        return chargeback;
    }

    @Override
    public void close() {

    }

}
