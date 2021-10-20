package com.rbkmoney.fraudbusters.mg.connector;

import com.rbkmoney.damsel.domain.InvoicePaid;
import com.rbkmoney.damsel.domain.InvoicePaymentProcessed;
import com.rbkmoney.damsel.domain.InvoicePaymentStatus;
import com.rbkmoney.damsel.domain.InvoiceStatus;
import com.rbkmoney.damsel.fraudbusters.Payment;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.fraudbusters.mg.connector.deserializer.SinkEventDeserializer;
import com.rbkmoney.fraudbusters.mg.connector.serde.deserializer.PaymentDeserializer;
import com.rbkmoney.fraudbusters.mg.connector.utils.BuildUtils;
import com.rbkmoney.fraudbusters.mg.connector.utils.MgEventSinkFlowGenerator;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;

//Test for real big data set
//Need get truststore pcsng-kafka by devops
@Slf4j
@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = FraudbustersMgConnectorApplication.class)
public class IntegrationTest extends KafkaAbstractTest {

    private static final String PKCS_12 = "PKCS12";
    @MockBean
    InvoicingSrv.Iface invoicingClient;

    public static <T> Consumer<String, T> createConsumerRemote() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                new File("src/test/resources/broker/pcsng-kafka.p12").getAbsolutePath());
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "xxx");
        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, PKCS_12);
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, PKCS_12);
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
                new File("src/test/resources/broker/strug.p12").getAbsolutePath());
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "xxx");
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "xxx");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "dev-kafka-mirror.bst1.rbkmoney.net:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SinkEventDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "fraud-connector");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5);
        return new KafkaConsumer<>(props);
    }

    @Test
    public void contextLoads() throws TException, IOException, InterruptedException {
        Mockito.when(invoicingClient.get(any(), any(), any()))
                .thenThrow(new RuntimeException())
                .thenReturn(BuildUtils.buildInvoice(MgEventSinkFlowGenerator.PARTY_ID, MgEventSinkFlowGenerator.SHOP_ID,
                        "sourceId", "1", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()),
                        InvoicePaymentStatus.processed(new InvoicePaymentProcessed())));
        int size = rewriteDataFromRealTopic();

        Consumer<String, Payment> consumerPayment = createPaymentConsumer(PaymentDeserializer.class);
        consumerPayment.subscribe(Collections.singletonList(PAYMENT));
        ArrayList<Payment> machineEvents = new ArrayList<>();

        int i = -1;
        while (i != 0) {
            try {
                ConsumerRecords<String, Payment> poll = consumerPayment.poll(Duration.ofSeconds(5));
                poll.iterator().forEachRemaining(payment -> machineEvents.add(payment.value()));
                i = poll.count();
            } catch (Exception e) {
                log.error("KafkaAbstractTest initialize e: ", e);
            }
        }

        log.debug("machineEvents size: {}", machineEvents.size());
        log.debug("machineEvents: {}", machineEvents);
    }

    private int rewriteDataFromRealTopic() throws InterruptedException {
        int i = 0;
        Consumer<String, SinkEvent> consumer = createConsumerRemote();
        consumer.subscribe(List.of("mg-events-invoice"));
        while (i < 50000) {
            ConsumerRecords<String, SinkEvent> poll = consumer.poll(Duration.ofSeconds(10));
            try (Producer<String, SinkEvent> producer = createProducer()) {
                for (ConsumerRecord<String, SinkEvent> stringObjectConsumerRecord : poll) {
                    ProducerRecord<String, SinkEvent> producerRecord = new ProducerRecord<>(MG_EVENT,
                            stringObjectConsumerRecord.key(), stringObjectConsumerRecord.value());
                    producer.send(producerRecord).get();
                    log.info("produceMessageToEventSink() sinkEvent: {}", stringObjectConsumerRecord.value());
                    i++;
                }
            } catch (Exception e) {
                log.error("Error when produceMessageToEventSink e:", e);
            }

        }
        Thread.sleep(2000L);

        return i;
    }
}
