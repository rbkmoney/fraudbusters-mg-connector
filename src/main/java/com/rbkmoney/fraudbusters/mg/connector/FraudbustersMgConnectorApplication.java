package com.rbkmoney.fraudbusters.mg.connector;

import com.rbkmoney.fraudbusters.mg.connector.pool.EventSinkStreamsPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

import javax.annotation.PreDestroy;

@ServletComponentScan
@SpringBootApplication
public class FraudbustersMgConnectorApplication extends SpringApplication {

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private EventSinkStreamsPool eventSinkStreamsPool;

    public static void main(String[] args) {
        SpringApplication.run(FraudbustersMgConnectorApplication.class, args);
    }

    @PreDestroy
    public void preDestroy() {
        eventSinkStreamsPool.clean();
        kafkaListenerEndpointRegistry.stop();
    }

}
