package com.rbkmoney.fraudbusters.mg.connector.pool;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    public void restartAllShutdownStreams() {
        if (!CollectionUtils.isEmpty(kafkaStreamsList)) {
            kafkaStreamsList.stream()
                    .filter(kafkaStreams -> kafkaStreams.state() == KafkaStreams.State.PENDING_SHUTDOWN)
                    .forEach(KafkaStreams::start);
        }
    }

    public void clean() {
        kafkaStreamsList.forEach(kafkaStreams -> kafkaStreams.close(Duration.ofSeconds(5L)));
    }

}
