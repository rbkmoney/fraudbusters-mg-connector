package com.rbkmoney.fraudbusters.mg.connector.parser;

import com.rbkmoney.machinegun.eventsink.MachineEvent;

public interface EventParser<T> {

    T parseEvent(MachineEvent message);

}
