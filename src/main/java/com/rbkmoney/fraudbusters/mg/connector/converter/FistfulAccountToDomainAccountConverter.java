package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.damsel.fraudbusters.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FistfulAccountToDomainAccountConverter
        implements Converter<com.rbkmoney.fistful.account.Account, Account> {

    private final FistfulCurrencyToDomainCurrencyConverter convertCurrency;

    @Override
    public Account convert(com.rbkmoney.fistful.account.Account fistfulAccount) {
        log.debug("Start convert fistfulAccount : {}", fistfulAccount);
        final Account account = new Account();
        account.setCurrency(convertCurrency.convert(fistfulAccount.getCurrency()));
        account.setId(fistfulAccount.getId());
        account.setIdentity(fistfulAccount.getIdentity());
        log.debug("Finish convert fistfulAccount : {} to domainAccount: {}", fistfulAccount, account);
        return account;
    }

}
