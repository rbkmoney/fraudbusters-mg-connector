package com.rbkmoney.fraudbusters.mg.connector.config;

import com.rbkmoney.fraudbusters.mg.connector.serde.MachineEventSerde;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaConfig {

    private static final String APP_ID = "fraud-connector";
    private static final String CLIENT_ID = "fraud-connector-client";
    private static final String PKCS_12 = "PKCS12";
    public static final String PAYOUT_SUFFIX = "-payout";

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    @Value("${kafka.ssl.server-password}")
    private String serverStorePassword;
    @Value("${kafka.ssl.server-keystore-location}")
    private String serverStoreCertPath;
    @Value("${kafka.ssl.keystore-password}")
    private String keyStorePassword;
    @Value("${kafka.ssl.key-password}")
    private String keyPassword;
    @Value("${kafka.ssl.keystore-location}")
    private String clientStoreCertPath;
    @Value("${kafka.ssl.enable}")
    private boolean kafkaSslEnable;
    @Value("${kafka.num-stream-threads}")
    private int numStreamThreads;

    @Bean
    public Properties mgInvoiceEventStreamProperties() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, MachineEventSerde.class);
        addDefaultProperties(props);
        props.putAll(sslConfigure());
        return props;
    }

    @Bean
    public Properties mgWithdrawalEventStreamProperties() {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID + PAYOUT_SUFFIX);
        props.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID + PAYOUT_SUFFIX);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, MachineEventSerde.class);
        addDefaultProperties(props);
        props.putAll(sslConfigure());
        return props;
    }

    private void addDefaultProperties(Properties props) {
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, numStreamThreads);
        props.put(StreamsConfig.RETRIES_CONFIG, 5);
        props.put(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler.class);
    }

    private Map<String, Object> sslConfigure() {
        Map<String, Object> configProps = new HashMap<>();
        if (kafkaSslEnable) {
            configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, new File(serverStoreCertPath).getAbsolutePath());
            configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, serverStorePassword);
            configProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, PKCS_12);
            configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, PKCS_12);
            configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, new File(clientStoreCertPath).getAbsolutePath());
            configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyStorePassword);
            configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
        }
        return configProps;
    }

}
