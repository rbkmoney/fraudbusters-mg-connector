package com.rbkmoney.fraudbusters.mg.connector.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashFlowResult {

    public static final CashFlowResult EMPTY = new CashFlowResult(0L, 0L, 0L, 0L, 0L);

    private final long amount;
    private final long guaranteeDeposit;
    private final long systemFee;
    private final long providerFee;
    private final long externalFee;
}
