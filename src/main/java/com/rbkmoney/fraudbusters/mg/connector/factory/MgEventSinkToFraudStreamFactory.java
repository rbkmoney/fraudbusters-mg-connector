package com.rbkmoney.fraudbusters.mg.connector.factory;

import com.rbkmoney.fraudbusters.mg.connector.domain.MgEventWrapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.SourceEventParser;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.ChargebackPaymentMapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.PaymentMapper;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.RefundPaymentMapper;
import com.rbkmoney.fraudbusters.mg.connector.serde.ChargebackSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.MachineEventSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.PaymentSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.RefundSerde;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.reflections.Reflections.log;

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

    private final MachineEventSerde machineEventSerde = new MachineEventSerde();
    private final PaymentMapper paymentMapper;
    private final ChargebackPaymentMapper chargebackPaymentMapper;
    private final RefundPaymentMapper refundPaymentMapper;
    private final SourceEventParser eventParser;

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
                                    .map(invoiceChange -> MgEventWrapper.builder()
                                            .change(invoiceChange)
                                            .event(entry.getKey())
                                            .build())
                                    .collect(Collectors.toList()))
                            .branch((id, change) -> paymentMapper.accept(change.getChange()),
                                    (id, change) -> chargebackPaymentMapper.accept(change.getChange()),
                                    (id, change) -> refundPaymentMapper.accept(change.getChange())
                            );

            branch[0].mapValues(mgEventWrapper -> paymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent()))
                    .peek((s, payment) -> log.debug("MgEventSinkToFraudStreamFactory payment: {}", payment))
                    .to(paymentTopic, Produced.with(Serdes.String(), paymentSerde));

            branch[1].mapValues(mgEventWrapper -> chargebackPaymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent()))
                    .peek((s, chargeback) -> log.debug("MgEventSinkToFraudStreamFactory chargeback: {}", chargeback))
                    .to(chargebackTopic, Produced.with(Serdes.String(), chargebackSerde));

            branch[2].mapValues(mgEventWrapper -> refundPaymentMapper.map(mgEventWrapper.getChange(), mgEventWrapper.getEvent()))
                    .peek((s, refund) -> log.debug("MgEventSinkToFraudStreamFactory refund: {}", refund))
                    .to(refundTopic, Produced.with(Serdes.String(), refundSerde));

            return new KafkaStreams(builder.build(), streamsConfiguration);
        } catch (Exception e) {
            log.error("WbListStreamFactory error when create stream e: ", e);
            throw new RuntimeException(e);
        }
    }

}
