package com.rbkmoney.fraudbusters.mg.connector.utils;

import com.rbkmoney.damsel.domain.AllocationTransaction;
import com.rbkmoney.damsel.domain.AllocationTransactionTargetShop;
import com.rbkmoney.damsel.fraudbusters.MerchantInfo;
import com.rbkmoney.damsel.fraudbusters.Payment;
import com.rbkmoney.damsel.fraudbusters.ReferenceInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper.DELIMITER;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentMapperUtils {

    public static Payment mapAllocationTransactionToPayment(Payment sourcePayment,
                                                            AllocationTransaction allocatedTrx) {
        Payment allocatedPayment = sourcePayment.deepCopy();
        allocatedPayment.setCost(allocatedTrx.getAmount());
        allocatedPayment.setId(String.join(DELIMITER, allocatedPayment.getId(), allocatedTrx.getId()));
        AllocationTransactionTargetShop targetShop = allocatedTrx.getTarget().getShop();
        allocatedPayment.setReferenceInfo(ReferenceInfo.merchant_info(new MerchantInfo()
                .setPartyId(targetShop.getOwnerId())
                .setShopId(targetShop.getShopId())
        ));
        return allocatedPayment;
    }

}
