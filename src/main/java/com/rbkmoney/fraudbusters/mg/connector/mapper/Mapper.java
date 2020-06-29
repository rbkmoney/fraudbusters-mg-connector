package com.rbkmoney.fraudbusters.mg.connector.mapper;


import com.rbkmoney.fraudbusters.mg.connector.constant.EventType;

public interface Mapper<C, P, R> {

    default boolean accept(C change) {
        return getChangeType().getFilter().match(change);
    }

    R map(C change, P parent);

    EventType getChangeType();

}
