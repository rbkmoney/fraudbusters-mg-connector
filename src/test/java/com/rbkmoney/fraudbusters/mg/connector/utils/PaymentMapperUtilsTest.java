package com.rbkmoney.fraudbusters.mg.connector.utils;

import com.rbkmoney.damsel.domain.AllocationTransaction;
import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.fraudbusters.Payment;
import org.junit.Test;

import static com.rbkmoney.fraudbusters.mg.connector.utils.BuildUtils.createTestAllocationTransaction;
import static com.rbkmoney.fraudbusters.mg.connector.utils.BuildUtils.createTestPayment;
import static org.junit.Assert.assertEquals;

public final class PaymentMapperUtilsTest {

    @Test
    public void mapAllocationTransactionToPaymentTest() {
        String testPaymentId = "id";
        Payment testPayment = createTestPayment(testPaymentId, new Cash().setAmount(1122L), "party-1", "shop-1");

        Cash allocCash = new Cash().setAmount(2000L);
        String allocParty = "party-2";
        String allocShop = "shop-2";
        String allocTrxId = "alloc-id";
        AllocationTransaction transaction =
                createTestAllocationTransaction(allocTrxId, allocCash, allocParty, allocShop);

        Payment resultPayment = PaymentMapperUtils.mapAllocationTransactionToPayment(testPayment, transaction);
        assertEquals(testPaymentId + "." + allocTrxId, resultPayment.getId());
        assertEquals(allocCash, resultPayment.getCost());
        assertEquals(allocParty, resultPayment.getReferenceInfo().getMerchantInfo().getPartyId());
        assertEquals(allocShop, resultPayment.getReferenceInfo().getMerchantInfo().getShopId());
    }

}
