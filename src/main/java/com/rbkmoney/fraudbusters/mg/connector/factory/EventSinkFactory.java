package com.rbkmoney.fraudbusters.mg.connector.factory;

import org.apache.kafka.streams.KafkaStreams;

public interface EventSinkFactory {

    KafkaStreams create();

}
