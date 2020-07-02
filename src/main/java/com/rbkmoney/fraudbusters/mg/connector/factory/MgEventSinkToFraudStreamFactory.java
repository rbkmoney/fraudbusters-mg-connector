package com.rbkmoney.fraudbusters.mg.connector.factory;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.fraudbusters.mg.connector.domain.MgEventWrapper;
import com.rbkmoney.fraudbusters.mg.connector.exception.StreamInitializationException;
import com.rbkmoney.fraudbusters.mg.connector.mapper.SourceEventParser;
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
public class MgEventSinkToFraudStreamFactory {

    @Value("${kafka.topic.mg-event}")
    private String readTopic;
    @Value("${kafka.topic.refund}")
    private String refundTopic;
    @Value("${kafka.topic.payment}")
    private String paymentTopic;
    @Value("${kafka.topic.chargeback}")
    private String chargebackTopic;

    private final Serde<MachineEvent> machineEventSerde = new MachineEventSerde();
    private final PaymentMapper paymentMapper;
    private final ChargebackPaymentMapper chargebackPaymentMapper;
    private final RefundPaymentMapper refundPaymentMapper;
    private final SourceEventParser eventParser;
    private final RetryTemplate retryTemplate;

    private final PaymentSerde paymentSerde = new PaymentSerde();
    private final RefundSerde refundSerde = new RefundSerde();
    private final ChargebackSerde chargebackSerde = new ChargebackSerde();

    public KafkaStreams create(final Properties streamsConfiguration) {
        try {
            StreamsBuilder builder = new StreamsBuilder();
            KStream<String, MgEventWrapper>[] branch =
                    builder.stream(readTopic, Consumed.with(Serdes.String(), machineEventSerde))
                            .mapValues(machineEvent -> Map.entry(machineEvent, eventParser.parseEvent(machineEvent)))
                            .filter((s, entry) -> entry.getValue().isSetInvoiceChanges())
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
                    .peek((s, payment) -> log.debug("MgEventSinkToFraudStreamFactory payment: {}", payment))
                    .to(paymentTopic, Produced.with(Serdes.String(), paymentSerde));

            branch[1].mapValues(mgEventWrapper ->
                    retryTemplate.execute(args ->
                            chargebackPaymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent())))
                    .peek((s, chargeback) -> log.debug("MgEventSinkToFraudStreamFactory chargeback: {}", chargeback))
                    .to(chargebackTopic, Produced.with(Serdes.String(), chargebackSerde));

            branch[2].mapValues(mgEventWrapper ->
                    retryTemplate.execute(args ->
                            refundPaymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent())))
                    .peek((s, refund) -> log.debug("MgEventSinkToFraudStreamFactory refund: {}", refund))
                    .to(refundTopic, Produced.with(Serdes.String(), refundSerde));

            return new KafkaStreams(builder.build(), streamsConfiguration);
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
