package com.rbkmoney.fraudbusters.mg.connector;

import com.rbkmoney.fraudbusters.mg.connector.listener.StartupListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import javax.annotation.PreDestroy;

@ServletComponentScan
@SpringBootApplication
public class FraudbustersMgConnectorApplication extends SpringApplication {

    @Autowired
    private StartupListener startupListener;

    public static void main(String[] args) {
        SpringApplication.run(FraudbustersMgConnectorApplication.class, args);
    }

    @PreDestroy
    public void preDestroy() {
        startupListener.stop();
    }

}
