package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.damsel.domain.BankCard;
import com.rbkmoney.damsel.domain.BankCardPaymentSystem;
import com.rbkmoney.damsel.domain.Residence;
import com.rbkmoney.damsel.fraudbusters.CryptoWallet;
import com.rbkmoney.damsel.fraudbusters.Resource;
import com.rbkmoney.fraudbusters.mg.connector.exception.UnknownResourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FistfulResourceToDomainResourceConverter implements Converter<com.rbkmoney.fistful.base.Resource, Resource> {

    @Override
    public Resource convert(com.rbkmoney.fistful.base.Resource fistfulResource) {
        log.debug("Start convert fistfulResource : {}", fistfulResource);
        final Resource resource = new Resource();
        if (fistfulResource.isSetBankCard()) {
            final BankCard bankCard = convertBankCard(fistfulResource.getBankCard().getBankCard());
            resource.setBankCard(bankCard);
        } else if (fistfulResource.isSetCryptoWallet()) {
            final CryptoWallet cryptoWallet = new CryptoWallet();
            resource.setCryptoWallet(cryptoWallet);
        } else {
            log.error("Unknown resource type: {}", fistfulResource);
            throw new UnknownResourceException();
        }
        log.debug("Finish convert fistfulResource : {} to domainResource: {}", fistfulResource, resource);
        return resource;
    }

    private BankCard convertBankCard(com.rbkmoney.fistful.base.BankCard bankCardFrom) {
        final BankCard bankCard = new BankCard();
        bankCard.setToken(bankCardFrom.getToken());
        bankCard.setIssuerCountry(Residence.valueOf(bankCardFrom.getIssuerCountry().name()));
        bankCard.setPaymentSystem(BankCardPaymentSystem.valueOf(bankCardFrom.getPaymentSystem().name()));
        bankCard.setLastDigits(bankCardFrom.getMaskedPan());
        bankCard.setBin(bankCardFrom.getBin());
        bankCard.setCategory(bankCardFrom.getCategory());
        bankCard.setBankName(bankCardFrom.getBankName());
        return bankCard;
    }
}
