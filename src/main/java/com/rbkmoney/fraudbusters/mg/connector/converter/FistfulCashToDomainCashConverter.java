package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.damsel.domain.Cash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FistfulCashToDomainCashConverter implements Converter<com.rbkmoney.fistful.base.Cash, Cash> {

    private final FistfulCurrencyToDomainCurrencyConverter convertCurrency;

    @Override
    public Cash convert(com.rbkmoney.fistful.base.Cash cash) {
        final com.rbkmoney.fistful.base.CurrencyRef currency = cash.getCurrency();
        return new Cash()
                .setAmount(cash.getAmount())
                .setCurrency(convertCurrency.convert(currency));
    }

}
