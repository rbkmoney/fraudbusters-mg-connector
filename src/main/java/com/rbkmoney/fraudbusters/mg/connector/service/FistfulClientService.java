package com.rbkmoney.fraudbusters.mg.connector.service;


import com.rbkmoney.fistful.withdrawal.ManagementSrv;
import com.rbkmoney.fistful.withdrawal.WithdrawalState;
import com.rbkmoney.fraudbusters.mg.connector.exception.PaymentInfoNotFoundException;
import com.rbkmoney.fraudbusters.mg.connector.exception.PaymentInfoRequestException;
import com.rbkmoney.fraudbusters.mg.connector.factory.FistfulEventRangeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FistfulClientService {

    private final ManagementSrv.Iface fistfulClient;
    private final FistfulEventRangeFactory fistfulEventRangeFactory;

    public WithdrawalState getWithdrawalInfoFromFistful(String eventId, long sequenceId) {
        try {
            final WithdrawalState withdrawalState = fistfulClient.get(eventId, fistfulEventRangeFactory.create(sequenceId));
            if (withdrawalState == null) {
                throw new PaymentInfoNotFoundException("Not found invoice info in hg!");
            }
            return withdrawalState;
        } catch (TException e) {
            log.error("Error when HgClientService getInvoiceInfo eventId: {} sequenceId: {} e: ", eventId, sequenceId, e);
            throw new PaymentInfoRequestException(e);
        }
    }
}
