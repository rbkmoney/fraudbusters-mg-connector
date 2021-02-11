package com.rbkmoney.fraudbusters.mg.connector.pool;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class EventSinkStreamsPool {

    private final List<KafkaStreams> kafkaStreamsList = Collections.synchronizedList(new ArrayList<>());

    public void add(KafkaStreams kafkaStreams) {
        kafkaStreamsList.add(kafkaStreams);
    }

    public void clean() {
        kafkaStreamsList.forEach(kafkaStreams -> {
            kafkaStreams.close(Duration.ofSeconds(5L));
        });
    }

}
