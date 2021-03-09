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

    public static final BankCardPaymentSystem DEAFAULT_PAYMENT_SYSTEM = BankCardPaymentSystem.visa;
    public static final String UNKNOWN = "UNKNOWN";

    @Override
    public Resource convert(com.rbkmoney.fistful.base.Resource fistfulResource) {
        log.debug("Start convert fistfulResource : {}", fistfulResource);
        Resource resource = new Resource();
        if (fistfulResource.isSetBankCard()) {
            BankCard bankCard = convertBankCard(fistfulResource.getBankCard().getBankCard());
            resource.setBankCard(bankCard);
        } else if (fistfulResource.isSetCryptoWallet()) {
            CryptoWallet cryptoWallet = new CryptoWallet()
                    .setId(fistfulResource.getCryptoWallet().getCryptoWallet().getId())
                    .setCurrency(fistfulResource.getCryptoWallet().getCryptoWallet().getCurrency().name());
            resource.setCryptoWallet(cryptoWallet);
        } else {
            log.error("Unknown resource type: {}", fistfulResource);
            throw new UnknownResourceException();
        }
        log.debug("Finish convert fistfulResource : {} to domainResource: {}", fistfulResource, resource);
        return resource;
    }

    private BankCard convertBankCard(com.rbkmoney.fistful.base.BankCard bankCardFrom) {
        BankCard bankCard = new BankCard();
        bankCard.setToken(bankCardFrom.getToken());
        bankCard.setIssuerCountry(bankCardFrom.isSetIssuerCountry() ?
                Residence.valueOf(bankCardFrom.getIssuerCountry().name()) : null);
        bankCard.setPaymentSystem(bankCardFrom.isSetPaymentSystem() ?
                BankCardPaymentSystem.valueOf(bankCardFrom.getPaymentSystem().name()) : DEAFAULT_PAYMENT_SYSTEM);
        bankCard.setLastDigits(bankCardFrom.getMaskedPan() != null ? bankCardFrom.getMaskedPan() : UNKNOWN);
        bankCard.setBin(bankCardFrom.getBin() != null ? bankCardFrom.getBin() : UNKNOWN);
        bankCard.setCategory(bankCardFrom.getCategory());
        bankCard.setBankName(bankCardFrom.getBankName());
        return bankCard;
    }
}
