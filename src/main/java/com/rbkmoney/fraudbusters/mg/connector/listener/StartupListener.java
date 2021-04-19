package com.rbkmoney.fraudbusters.mg.connector.listener;

import com.rbkmoney.fraudbusters.mg.connector.factory.EventSinkFactory;
import com.rbkmoney.fraudbusters.mg.connector.pool.EventSinkStreamsPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private final List<EventSinkFactory> eventSinkFactories;
    private final EventSinkStreamsPool eventSinkStreamsPool;

    @Value("${kafka.stream.clean-timeout-sec}")
    private Long cleanTimeoutSec;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!CollectionUtils.isEmpty(eventSinkFactories)) {
            eventSinkFactories.forEach(this::initKafkaStream);
        }
    }

    private void initKafkaStream(EventSinkFactory eventSinkFactory) {
        KafkaStreams kafkaStreams = eventSinkFactory.create();
        kafkaStreams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> kafkaStreams.close(Duration.ofSeconds(cleanTimeoutSec))));
        eventSinkStreamsPool.put(eventSinkFactory.getType(), kafkaStreams);
        log.info("StartupListener start stream kafkaStreams: {}", kafkaStreams.allMetadata());
    }

}
