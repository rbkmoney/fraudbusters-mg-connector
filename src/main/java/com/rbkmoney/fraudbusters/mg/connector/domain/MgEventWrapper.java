package com.rbkmoney.fraudbusters.mg.connector.domain;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MgEventWrapper {

    private InvoiceChange change;
    private MachineEvent event;

}
