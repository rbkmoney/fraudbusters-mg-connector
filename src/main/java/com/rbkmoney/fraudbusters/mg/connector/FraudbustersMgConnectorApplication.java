package com.rbkmoney.fraudbusters.mg.connector;

import com.rbkmoney.fraudbusters.mg.connector.listener.StreamStateManager;
import com.rbkmoney.fraudbusters.mg.connector.pool.EventSinkStreamsPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;

@EnableScheduling
@ServletComponentScan
@SpringBootApplication
public class FraudbustersMgConnectorApplication extends SpringApplication {

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private EventSinkStreamsPool eventSinkStreamsPool;

    @Autowired
    private StreamStateManager streamStateManager;

    public static void main(String[] args) {
        SpringApplication.run(FraudbustersMgConnectorApplication.class, args);
    }

    @PreDestroy
    public void preDestroy() {
        streamStateManager.stop();
        eventSinkStreamsPool.cleanAll();
        kafkaListenerEndpointRegistry.stop();
    }

}
