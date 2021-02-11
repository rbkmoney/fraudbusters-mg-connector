package com.rbkmoney.fraudbusters.mg.connector.listener;

import com.rbkmoney.fraudbusters.mg.connector.factory.EventSinkFactory;
import com.rbkmoney.fraudbusters.mg.connector.pool.EventSinkStreamsPool;
import com.rbkmoney.fraudbusters.mg.connector.utils.ShutdownManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    public static final int FATAL_ERROR_CODE_IN_STREAM = 228;
    private final List<EventSinkFactory> eventSinkFactories;
    private final EventSinkStreamsPool eventSinkStreamsPool;
    private final ShutdownManager shutdownManager;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!CollectionUtils.isEmpty(eventSinkFactories)) {
            eventSinkFactories.forEach(this::initKafkaStream);
        }
    }

    private void initKafkaStream(EventSinkFactory eventSinkFactory) {
        KafkaStreams kafkaStreams = eventSinkFactory.create();
        kafkaStreams.setUncaughtExceptionHandler(this::handleCriticalError);
        kafkaStreams.start();
        eventSinkStreamsPool.add(kafkaStreams);
        log.info("StartupListener start stream kafkaStreams: {}", kafkaStreams.allMetadata());
    }

    private void handleCriticalError(Thread t, Throwable e) {
        log.error("Unhandled exception in " + t.getName() + ", exiting. {}", eventSinkStreamsPool, e);
        shutdownManager.initiateShutdown(FATAL_ERROR_CODE_IN_STREAM);
    }

}
