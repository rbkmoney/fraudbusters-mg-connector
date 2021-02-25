package com.rbkmoney.fraudbusters.mg.connector.service;


import com.rbkmoney.fistful.base.EventRange;
import com.rbkmoney.fistful.wallet.ManagementSrv;
import com.rbkmoney.fistful.wallet.WalletState;
import com.rbkmoney.fraudbusters.mg.connector.exception.PaymentInfoNotFoundException;
import com.rbkmoney.fraudbusters.mg.connector.exception.PaymentInfoRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletClientService {

    private final ManagementSrv.Iface walletClient;

    public WalletState getWalletInfoFromFistful(String eventId) {
        try {
            final WalletState walletState = walletClient.get(eventId, new EventRange());
            if (walletState == null) {
                throw new PaymentInfoNotFoundException("Not found invoice info in hg!");
            }
            return walletState;
        } catch (TException e) {
            log.error("Error when getWalletInfoFromFistful eventId: {} e: ", eventId, e);
            throw new PaymentInfoRequestException(e);
        }
    }
}
