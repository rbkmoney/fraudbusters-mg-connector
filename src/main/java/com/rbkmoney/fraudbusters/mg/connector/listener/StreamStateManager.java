package com.rbkmoney.fraudbusters.mg.connector.listener;

import com.rbkmoney.fraudbusters.mg.connector.constant.StreamType;
import com.rbkmoney.fraudbusters.mg.connector.factory.EventSinkFactory;
import com.rbkmoney.fraudbusters.mg.connector.pool.EventSinkStreamsPool;
import com.rbkmoney.fraudbusters.mg.connector.utils.ShutdownManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamStateManager {

    public static final int FATAL_ERROR_CODE_IN_STREAM = 228;

    private AtomicBoolean isStreamRunning = new AtomicBoolean(true);

    private final EventSinkStreamsPool eventSinkStreamsPool;
    private final ShutdownManager shutdownManager;
    private final List<EventSinkFactory> eventSinkFactories;

    @Scheduled(fixedDelayString = "${kafka.stream.fixed-rate-timeout-ms}")
    public void monitorStateOfStreams() {
        if (isStreamRunning.get()) {
            try {
                eventSinkFactories.forEach(this::createStreamIfShutdown);
            } catch (Exception e) {
                log.error("Error in monitor shutdown streams. {}", eventSinkStreamsPool, e);
                shutdownManager.initiateShutdown(FATAL_ERROR_CODE_IN_STREAM);
            }
        }
    }

    public void stop() {
        isStreamRunning.set(false);
    }

    private void createStreamIfShutdown(EventSinkFactory eventSinkFactory) {
        StreamType streamType = eventSinkFactory.getType();
        KafkaStreams kafkaStreams = eventSinkStreamsPool.get(streamType);
        if (kafkaStreams != null && (kafkaStreams.state() == KafkaStreams.State.NOT_RUNNING)) {
            KafkaStreams kafkaStreamsNew = eventSinkFactory.create();
            kafkaStreamsNew.start();
            eventSinkStreamsPool.put(streamType, kafkaStreamsNew);
            log.info("Kafka stream streamType: {} state: {}", streamType, kafkaStreams.state());
        }
    }

}
