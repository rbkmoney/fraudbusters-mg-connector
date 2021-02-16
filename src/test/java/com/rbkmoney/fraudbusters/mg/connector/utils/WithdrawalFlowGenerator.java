package com.rbkmoney.fraudbusters.mg.connector.utils;

import com.rbkmoney.fistful.base.Failure;
import com.rbkmoney.fistful.withdrawal.Change;
import com.rbkmoney.fistful.withdrawal.StatusChange;
import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fistful.withdrawal.status.Failed;
import com.rbkmoney.fistful.withdrawal.status.Pending;
import com.rbkmoney.fistful.withdrawal.status.Status;
import com.rbkmoney.fistful.withdrawal.status.Succeeded;
import com.rbkmoney.geck.common.util.TypeUtil;
import com.rbkmoney.kafka.common.serialization.ThriftSerializer;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import com.rbkmoney.machinegun.msgpack.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WithdrawalFlowGenerator {

    public static final String SOURCE_NS = "SOURCE_NS";

    public static List<SinkEvent> generateSuccessFlow(String sourceId) {
        final ArrayList<SinkEvent> sinkEvents = new ArrayList<>();
        Long sequenceId = 0L;
        sinkEvents.add(createSinkEvent(createMachineEvent(createTimestampedChange(createPendingStatus()), sourceId, sequenceId++)));
        sinkEvents.add(createSinkEvent(createMachineEvent(createTimestampedChange(createPendingStatus()), sourceId, sequenceId++)));
        sinkEvents.add(createSinkEvent(createMachineEvent(createTimestampedChange(createSuccessStatus()), sourceId, sequenceId++)));
        sinkEvents.add(createSinkEvent(createMachineEvent(createTimestampedChange(createFailedStatus()), sourceId, sequenceId)));
        return sinkEvents;
    }

    private static Status createFailedStatus() {
        return new Status(Status.failed(new Failed()
                .setFailure(new Failure().setCode("code")
                        .setReason("reason"))));
    }

    private static Status createSuccessStatus() {
        return new Status(Status.succeeded(new Succeeded()));
    }

    private static Status createPendingStatus() {
        return new Status(Status.pending(new Pending()));
    }

    private static TimestampedChange createTimestampedChange(Status status) {
        final TimestampedChange timestampedChange = new TimestampedChange();
        final Change change = new Change();
        timestampedChange.setOccuredAt(Instant.now().toString());
        change.setStatusChanged(new StatusChange()
                .setStatus(status));
        timestampedChange.setChange(change);
        return timestampedChange;
    }

    private static SinkEvent createSinkEvent(MachineEvent machineEvent) {
        SinkEvent sinkEvent = new SinkEvent();
        sinkEvent.setEvent(machineEvent);
        return sinkEvent;
    }

    private static MachineEvent createMachineEvent(TimestampedChange timestampedChange, String sourceId, Long sequenceId) {
        MachineEvent message = new MachineEvent();
        message.setCreatedAt(TypeUtil.temporalToString(Instant.now()));
        message.setEventId(sequenceId);
        message.setSourceNs(SOURCE_NS);
        message.setSourceId(sourceId);

        ThriftSerializer<TimestampedChange> eventPayloadThriftSerializer = new ThriftSerializer<>();
        Value data = new Value();
        data.setBin(eventPayloadThriftSerializer.serialize("", timestampedChange));
        message.setData(data);
        return message;
    }

}
