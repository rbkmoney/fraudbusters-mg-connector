package com.rbkmoney.fraudbusters.mg.connector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Value("${kafka.stream.retries-attempts}")
    private int retriesAttempts;

    @Value("${kafka.stream.retries-backoff-ms}")
    private int retriesBackoffMs;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(retriesBackoffMs);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(retriesAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

}
