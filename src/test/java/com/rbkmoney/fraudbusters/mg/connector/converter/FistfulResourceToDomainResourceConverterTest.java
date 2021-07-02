package com.rbkmoney.fraudbusters.mg.connector.converter;

import com.rbkmoney.damsel.fraudbusters.Resource;
import com.rbkmoney.fistful.base.ResourceBankCard;
import com.rbkmoney.fistful.base.ResourceCryptoWallet;
import com.rbkmoney.fistful.base.ResourceDigitalWallet;
import com.rbkmoney.fraudbusters.mg.connector.exception.UnknownResourceException;
import com.rbkmoney.fraudbusters.mg.connector.utils.BuildUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class FistfulResourceToDomainResourceConverterTest {

    @Autowired
    private FistfulResourceToDomainResourceConverter converter;

    @Test
    void convertResourceBankCardTest() {
        ResourceBankCard resourceBankCard = new ResourceBankCard();
        resourceBankCard.setBankCard(BuildUtils.buildFistfulBankCard());
        com.rbkmoney.fistful.base.Resource baseResource = new com.rbkmoney.fistful.base.Resource();
        baseResource.setBankCard(resourceBankCard);

        Resource resource = converter.convert(baseResource);
        assertNotNull(resource);
        assertTrue(resource.isSetBankCard());
    }

    @Test
    void convertResourceCryptoWalletTest() {
        ResourceCryptoWallet resourceCryptoWallet = new ResourceCryptoWallet();
        resourceCryptoWallet.setCryptoWallet(BuildUtils.buildFistfulCryptoWallet());
        com.rbkmoney.fistful.base.Resource baseResource = new com.rbkmoney.fistful.base.Resource();
        baseResource.setCryptoWallet(resourceCryptoWallet);

        Resource resource = converter.convert(baseResource);
        assertNotNull(resource);
        assertTrue(resource.isSetCryptoWallet());
    }

    @Test
    void convertResourceDigitalWalletTest() {
        ResourceDigitalWallet resourceDigitalWallet = new ResourceDigitalWallet();
        resourceDigitalWallet.setDigitalWallet(BuildUtils.buildFistfulDigitalWallet());
        com.rbkmoney.fistful.base.Resource baseResource = new com.rbkmoney.fistful.base.Resource();
        baseResource.setDigitalWallet(resourceDigitalWallet);

        Resource resource = converter.convert(baseResource);
        assertNotNull(resource);
        assertTrue(resource.isSetDigitalWallet());
    }

    @Test
    void convertUnknownResourceExceptionTest() {
        Exception exception = assertThrows(UnknownResourceException.class, () -> {
            converter.convert(new com.rbkmoney.fistful.base.Resource());
        });
        assertNotNull(exception);
    }

}