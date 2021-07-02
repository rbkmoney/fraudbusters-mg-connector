package com.rbkmoney.fraudbusters.mg.connector.utils;

import com.rbkmoney.fistful.base.CardType;
import com.rbkmoney.fistful.base.LegacyBankCardPaymentSystem;
import com.rbkmoney.fistful.base.Residence;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class InvoiceTestConstant {

    public static final LegacyBankCardPaymentSystem CARD_PAYMENT_SYSTEM = LegacyBankCardPaymentSystem.mastercard;
    public static final String CARD_PAYMENT_SYSTEM_REF = "1";
    public static final String CARD_BIN = "bin";
    public static final String CARDHOLDER_NAME = "CARD HOLDER";
    public static final String CARD_MASKED_PAN = "1232132";
    public static final String CARD_LAST_DIGIT = "1234";
    public static final String CARD_EXP_DATE_MONTH = "12";
    public static final String CARD_EXP_DATE_YEAR = "2020";
    public static final String CARD_CATEGORY = "category";
    public static final String BANK_NAME = "bankName";
    public static final String CARD_TOKEN_PROVIDER = "cardToken";
    public static final Residence ISSUER_COUNTRY = Residence.PAN;
    public static final CardType CARD_TYPE = CardType.debit;

}
