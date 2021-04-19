package com.rbkmoney.fraudbusters.mg.connector.factory;

import com.rbkmoney.fraudbusters.mg.connector.constant.StreamType;
import org.apache.kafka.streams.KafkaStreams;

public interface EventSinkFactory {

    StreamType getType();

    KafkaStreams create();

}
