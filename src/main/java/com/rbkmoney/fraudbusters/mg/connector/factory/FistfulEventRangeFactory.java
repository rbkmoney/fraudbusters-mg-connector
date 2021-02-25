package com.rbkmoney.fraudbusters.mg.connector.factory;


import com.rbkmoney.fistful.base.EventRange;
import org.springframework.stereotype.Service;

@Service
public class FistfulEventRangeFactory {

    public EventRange create(long eventNumber) {
        return new EventRange()
                .setLimit((int) eventNumber);
    }

}
