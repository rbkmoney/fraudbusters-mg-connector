package com.rbkmoney.fraudbusters.mg.connector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
public class FraudbustersMgConnectorApplication extends SpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudbustersMgConnectorApplication.class, args);
    }

}
