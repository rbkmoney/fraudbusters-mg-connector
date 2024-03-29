package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.damsel.domain.BankCard;
import com.rbkmoney.damsel.domain.CountryCode;
import com.rbkmoney.damsel.domain.LegacyBankCardPaymentSystem;
import com.rbkmoney.damsel.domain.PaymentSystemRef;
import com.rbkmoney.damsel.fraudbusters.CryptoWallet;
import com.rbkmoney.damsel.fraudbusters.DigitalWallet;
import com.rbkmoney.damsel.fraudbusters.Resource;
import com.rbkmoney.fraudbusters.mg.connector.exception.UnknownResourceException;
import com.rbkmoney.mamsel.PaymentSystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FistfulResourceToDomainResourceConverter
        implements Converter<com.rbkmoney.fistful.base.Resource, Resource> {

    public static final PaymentSystemRef DEFAULT_PAYMENT_SYSTEM =
            new PaymentSystemRef(LegacyBankCardPaymentSystem.visa.name());
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
        } else if (fistfulResource.isSetDigitalWallet()) {
            DigitalWallet digitalWallet = new DigitalWallet()
                    .setId(fistfulResource.getDigitalWallet().getDigitalWallet().getId());
            if (fistfulResource.getDigitalWallet().getDigitalWallet().getData() != null) {
                digitalWallet.setDigitalDataProvider(fistfulResource.getDigitalWallet()
                        .getDigitalWallet().getData().getSetField().getFieldName());
            }
            resource.setDigitalWallet(digitalWallet);
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
        bankCard.setIssuerCountry(bankCardFrom.isSetIssuerCountry()
                ? CountryCode.valueOf(bankCardFrom.getIssuerCountry().name())
                : null);
        bankCard.setPaymentSystem(bankCardFrom.isSetPaymentSystem()
                ? new PaymentSystemRef(PaymentSystemUtil.getFistfulPaymentSystemName(bankCardFrom))
                : DEFAULT_PAYMENT_SYSTEM);
        bankCard.setLastDigits(bankCardFrom.getMaskedPan() != null
                ? bankCardFrom.getMaskedPan()
                : UNKNOWN);
        bankCard.setBin(bankCardFrom.getBin() != null
                ? bankCardFrom.getBin()
                : UNKNOWN);
        bankCard.setCategory(bankCardFrom.getCategory());
        bankCard.setBankName(bankCardFrom.getBankName());
        return bankCard;
    }
}
