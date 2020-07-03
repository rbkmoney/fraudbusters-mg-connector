package com.rbkmoney.fraudbusters.mg.connector.listener;

import com.rbkmoney.fraudbusters.mg.connector.factory.MgEventSinkToFraudStreamFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private final MgEventSinkToFraudStreamFactory sinkToFraudStreamFactory;
    private final Properties mgEventStreamProperties;
    private final KafkaListenerEndpointRegistry registry;

    private KafkaStreams kafkaStreams;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        kafkaStreams = sinkToFraudStreamFactory.create(mgEventStreamProperties);
        kafkaStreams.start();
        log.info("StartupListener start stream kafkaStreams: {}", kafkaStreams.allMetadata());
    }

    public void stop() {
        kafkaStreams.close(Duration.ofSeconds(1L));
        registry.stop();
    }

}