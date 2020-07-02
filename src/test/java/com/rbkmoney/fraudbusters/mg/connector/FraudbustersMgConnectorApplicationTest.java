package com.rbkmoney.fraudbusters.mg.connector;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.fraudbusters.Payment;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.fraudbusters.mg.connector.factory.EventRangeFactory;
import com.rbkmoney.fraudbusters.mg.connector.serde.deserializer.ChargebackDeserializer;
import com.rbkmoney.fraudbusters.mg.connector.serde.deserializer.PaymentDeserializer;
import com.rbkmoney.fraudbusters.mg.connector.serde.deserializer.RefundDeserializer;
import com.rbkmoney.fraudbusters.mg.connector.service.HgClientService;
import com.rbkmoney.fraudbusters.mg.connector.utils.BuildUtils;
import com.rbkmoney.fraudbusters.mg.connector.utils.MgEventSinkFlowGenerator;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FraudbustersMgConnectorApplication.class)
public class FraudbustersMgConnectorApplicationTest extends KafkaAbstractTest {

    public static final String SOURCE_ID = "source_id";
    public static final long TIMEOUT = 2000L;

    @MockBean
    InvoicingSrv.Iface invoicingClient;

    @Autowired
    private EventRangeFactory eventRangeFactory;

    @Test
    public void contextLoads() throws TException, IOException, InterruptedException {
        List<SinkEvent> sinkEvents = MgEventSinkFlowGenerator.generateSuccessFlow(SOURCE_ID);
        mockPayment(SOURCE_ID);
        sinkEvents.forEach(this::produceMessageToEventSink);
        checkMessageInTopic(PAYMENT, PaymentDeserializer.class, 2);

        String sourceID_refund_1 = "sourceID_refund_1";
        mockPayment(sourceID_refund_1);
        mockRefund(sourceID_refund_1, 7, "1");
        mockRefund(sourceID_refund_1, 9, "2");
        sinkEvents = MgEventSinkFlowGenerator.generateRefundedFlow(sourceID_refund_1);
        sinkEvents.forEach(this::produceMessageToEventSink);

        checkMessageInTopic(REFUND, RefundDeserializer.class, 2);

        String sourceChargeback = "source_chargeback";
        sinkEvents = MgEventSinkFlowGenerator.generateChargebackFlow(sourceChargeback);
        mockPayment(sourceChargeback);
        mockChargeback(sourceChargeback, 6, "1");
        sinkEvents.forEach(this::produceMessageToEventSink);

        checkMessageInTopic(CHARGEBACK, ChargebackDeserializer.class, 1);

        //check exceptions retry
        sinkEvents = MgEventSinkFlowGenerator.generateSuccessFlow(SOURCE_ID);
        mockPaymentWithException(SOURCE_ID);
        sinkEvents.forEach(this::produceMessageToEventSink);
        checkMessageInTopic(PAYMENT, PaymentDeserializer.class, 6);
    }

    private void checkMessageInTopic(String topicName, Class<?> clazz, int size) throws InterruptedException {
        Thread.sleep(TIMEOUT);

        Consumer<String, Payment> consumer = createPaymentConsumer(clazz);
        try {
            consumer.subscribe(Collections.singletonList(topicName));
            ConsumerRecords<String, Payment> poll = consumer.poll(Duration.ofSeconds(5));
            Assert.assertTrue(poll.iterator().hasNext());
            Assert.assertEquals(size, poll.count());
            log.info("message: {}", poll.iterator().next().value());
        } catch (Exception e) {
            log.error("KafkaAbstractTest initialize e: ", e);
        }
        consumer.close();
    }

    private void mockPayment(String sourceId) throws TException, IOException {
        mockPayment(sourceId, 4);
        mockPayment(sourceId, 5);
    }

    private void mockPaymentWithException(String sourceId) throws TException, IOException {
        Mockito.when(invoicingClient.get(HgClientService.USER_INFO, sourceId, eventRangeFactory.create(4)))
                .thenThrow(new RuntimeException())
                .thenReturn(BuildUtils.buildInvoice(MgEventSinkFlowGenerator.PARTY_ID, MgEventSinkFlowGenerator.SHOP_ID,
                        sourceId, "1", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()), InvoicePaymentStatus.processed(new InvoicePaymentProcessed())));
        mockPayment(sourceId, 5);
    }

    private OngoingStubbing<Invoice> mockPayment(String sourceId, int i) throws TException, IOException {
        return Mockito.when(invoicingClient.get(HgClientService.USER_INFO, sourceId, eventRangeFactory.create(i)))
                .thenReturn(BuildUtils.buildInvoice(MgEventSinkFlowGenerator.PARTY_ID, MgEventSinkFlowGenerator.SHOP_ID,
                        sourceId, "1", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()), InvoicePaymentStatus.processed(new InvoicePaymentProcessed())));
    }

    private void mockRefund(String sourceId, int sequenceId, String refundId) throws TException, IOException {
        Mockito.when(invoicingClient.get(HgClientService.USER_INFO, sourceId, eventRangeFactory.create(sequenceId)))
                .thenReturn(BuildUtils.buildInvoice(MgEventSinkFlowGenerator.PARTY_ID, MgEventSinkFlowGenerator.SHOP_ID,
                        sourceId, "1", refundId, "1",
                        InvoiceStatus.paid(new InvoicePaid()), InvoicePaymentStatus.refunded(new InvoicePaymentRefunded())));
    }

    private void mockChargeback(String sourceId, int sequenceId, String chargebackId) throws TException, IOException {
        Mockito.when(invoicingClient.get(HgClientService.USER_INFO, sourceId, eventRangeFactory.create(sequenceId)))
                .thenReturn(BuildUtils.buildInvoice(MgEventSinkFlowGenerator.PARTY_ID, MgEventSinkFlowGenerator.SHOP_ID,
                        sourceId, "1", "1", chargebackId,
                        InvoiceStatus.paid(new InvoicePaid()), InvoicePaymentStatus.charged_back(new InvoicePaymentChargedBack())));
    }

}
