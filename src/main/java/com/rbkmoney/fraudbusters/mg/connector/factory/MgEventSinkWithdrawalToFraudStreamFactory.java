package com.rbkmoney.fraudbusters.mg.connector.factory;

import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fraudbusters.mg.connector.exception.StreamInitializationException;
import com.rbkmoney.fraudbusters.mg.connector.mapper.impl.WithdrawalMapper;
import com.rbkmoney.fraudbusters.mg.connector.parser.EventParser;
import com.rbkmoney.fraudbusters.mg.connector.serde.MachineEventSerde;
import com.rbkmoney.fraudbusters.mg.connector.serde.WithdrawalSerde;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class MgEventSinkWithdrawalToFraudStreamFactory implements EventSinkFactory {

    @Value("${kafka.topic.source.withdrawal}")
    private String source;
    @Value("${kafka.topic.sink.withdrawal}")
    private String sink;

    private final Serde<MachineEvent> machineEventSerde = new MachineEventSerde();
    private final WithdrawalMapper withdrawalMapper;
    private final EventParser<TimestampedChange> withdrawalEventParser;
    private final RetryTemplate retryTemplate;
    private final WithdrawalSerde withdrawalSerde = new WithdrawalSerde();
    private final Properties mgWithdrawalEventStreamProperties;

    @Override
    public KafkaStreams create() {
        try {
            StreamsBuilder builder = new StreamsBuilder();

            builder.stream(source, Consumed.with(Serdes.String(), machineEventSerde))
                    .mapValues(machineEvent -> Map.entry(machineEvent, withdrawalEventParser.parseEvent(machineEvent)))
                    .filter((s, entry) -> filterChange(entry))
                    .mapValues(entry -> retryTemplate.execute(args ->
                                    withdrawalMapper.map(entry.getValue(), entry.getKey())
                            )
                    )
                    .to(sink, Produced.with(Serdes.String(), withdrawalSerde));

            return new KafkaStreams(builder.build(), mgWithdrawalEventStreamProperties);
        } catch (Exception e) {
            log.error("Error when create stream e: ", e);
            throw new StreamInitializationException(e);
        }
    }

    private boolean filterChange(Map.Entry<MachineEvent, TimestampedChange> entry) {
        log.debug("filterChange entry: {}", entry);
        return entry.getValue().isSetChange();
    }

}
