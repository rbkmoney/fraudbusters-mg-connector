package com.rbkmoney.fraudbusters.mg.connector.factory;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.fraudbusters.mg.connector.domain.MgEventWrapper;
import com.rbkmoney.fraudbusters.mg.connector.exception.StreamInitializationException;
import com.rbkmoney.fraudbusters.mg.connector.mapper.PaymentEventParser;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.ChargebackPaymentMapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.PaymentMapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.RefundPaymentMapper;
import com.rbkmoney.fraudbusters.mg.connector.serde.ChargebackSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.MachineEventSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.PaymentSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.RefundSerde;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MgEventSinkInvoiceToFraudStreamFactory implements EventSinkFactory {

    @Value("${kafka.topic.source.invoicing}")
    private String readTopic;
    @Value("${kafka.topic.sink.refund}")
    private String refundTopic;
    @Value("${kafka.topic.sink.payment}")
    private String paymentTopic;
    @Value("${kafka.topic.sink.chargeback}")
    private String chargebackTopic;

    private final Serde<MachineEvent> machineEventSerde = new MachineEventSerde();
    private final PaymentMapper paymentMapper;
    private final ChargebackPaymentMapper chargebackPaymentMapper;
    private final RefundPaymentMapper refundPaymentMapper;
    private final PaymentEventParser eventParser;
    private final RetryTemplate retryTemplate;

    private final PaymentSerde paymentSerde = new PaymentSerde();
    private final RefundSerde refundSerde = new RefundSerde();
    private final ChargebackSerde chargebackSerde = new ChargebackSerde();
    private final Properties mgInvoiceEventStreamProperties;

    @Override
    public KafkaStreams create() {
        try {
            StreamsBuilder builder = new StreamsBuilder();
            KStream<String, MgEventWrapper>[] branch =
                    builder.stream(readTopic, Consumed.with(Serdes.String(), machineEventSerde))
                            .mapValues(machineEvent -> Map.entry(machineEvent, eventParser.parseEvent(machineEvent)))
                            .peek((s, payment) -> log.debug("MgEventSinkToFraudStreamFactory machineEvent: {}", payment))
                            .filter((s, entry) -> entry.getValue().isSetInvoiceChanges())
                            .peek((s, payment) -> log.debug("MgEventSinkToFraudStreamFactory machineEvent: {}", payment))
                            .flatMapValues(entry -> entry.getValue().getInvoiceChanges().stream()
                                    .map(invoiceChange -> wrapMgEvent(entry, invoiceChange))
                                    .collect(Collectors.toList()))
                            .branch((id, change) -> paymentMapper.accept(change.getChange()),
                                    (id, change) -> chargebackPaymentMapper.accept(change.getChange()),
                                    (id, change) -> refundPaymentMapper.accept(change.getChange())
                            );

            branch[0].mapValues(mgEventWrapper ->
                    retryTemplate.execute(args ->
                            paymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent())))
                    .to(paymentTopic, Produced.with(Serdes.String(), paymentSerde));

            branch[1].mapValues(mgEventWrapper ->
                    retryTemplate.execute(args ->
                            chargebackPaymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent())))
                    .to(chargebackTopic, Produced.with(Serdes.String(), chargebackSerde));

            branch[2].mapValues(mgEventWrapper ->
                    retryTemplate.execute(args ->
                            refundPaymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent())))
                    .to(refundTopic, Produced.with(Serdes.String(), refundSerde));

            return new KafkaStreams(builder.build(), mgInvoiceEventStreamProperties);
        } catch (Exception e) {
            log.error("WbListStreamFactory error when create stream e: ", e);
            throw new StreamInitializationException(e);
        }
    }

    private MgEventWrapper wrapMgEvent(Map.Entry<MachineEvent, EventPayload> entry, InvoiceChange invoiceChange) {
        return MgEventWrapper.builder()
                .change(invoiceChange)
                .event(entry.getKey())
                .build();
    }

}
