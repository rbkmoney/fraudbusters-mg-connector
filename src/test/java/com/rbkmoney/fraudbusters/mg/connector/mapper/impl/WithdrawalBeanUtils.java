package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.fistful.account.Account;
import com.rbkmoney.fistful.base.*;
import com.rbkmoney.fistful.destination.DestinationState;
import com.rbkmoney.fistful.wallet.WalletState;
import com.rbkmoney.fistful.withdrawal.Change;
import com.rbkmoney.fistful.withdrawal.StatusChange;
import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fistful.withdrawal.status.Status;
import com.rbkmoney.fraudbusters.mg.connector.utils.InvoiceTestConstant;

public class WithdrawalBeanUtils {

    public static final String IDENTITY_ID = "identity_id";
    public static final String WALLET_ACCOUNT_ID = "wallet_account_id";
    public static final String RUB = "RUB";

    public static DestinationState createDestinationState() {
        final Resource resource = new Resource();
        resource.setBankCard(new ResourceBankCard()
                .setBankCard(new BankCard()
                        .setBankName(InvoiceTestConstant.BANK_NAME)
                        .setBin(InvoiceTestConstant.CARD_BIN)
                        .setCategory(InvoiceTestConstant.CARD_CATEGORY)
                        .setIssuerCountry(Residence.PAN)
                        .setPaymentSystem(new PaymentSystemRef(LegacyBankCardPaymentSystem.mastercard.name()))
                        .setToken(InvoiceTestConstant.CARD_TOKEN_PROVIDER)
                        .setMaskedPan(InvoiceTestConstant.CARD_MASKED_PAN)
                        .setCardType(CardType.debit)
                        .setCardholderName(InvoiceTestConstant.CARDHOLDER_NAME)));
        return new DestinationState().setResource(resource);
    }

    public static TimestampedChange createStatusCahnge(Status failed) {
        final Change change = new Change();
        final TimestampedChange timestampedChange = new TimestampedChange();
        change.setStatusChanged(new StatusChange()
                .setStatus(failed));

        timestampedChange.setChange(change);
        return timestampedChange;
    }

    public static WalletState createWallet() {
        return new WalletState()
                .setAccount(new Account()
                        .setCurrency(new CurrencyRef(RUB))
                        .setId(WALLET_ACCOUNT_ID)
                        .setIdentity(IDENTITY_ID));
    }

    public static Cash createCash() {
        return new Cash()
                .setAmount(100L)
                .setCurrency(new CurrencyRef(RUB));
    }

}
