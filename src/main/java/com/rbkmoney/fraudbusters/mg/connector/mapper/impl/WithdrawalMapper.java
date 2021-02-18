package com.rbkmoney.fraudbusters.mg.connector.mapper.impl;

import com.rbkmoney.damsel.fraudbusters.Resource;
import com.rbkmoney.damsel.fraudbusters.Withdrawal;
import com.rbkmoney.damsel.fraudbusters.WithdrawalStatus;
import com.rbkmoney.fistful.destination.DestinationState;
import com.rbkmoney.fistful.wallet.WalletState;
import com.rbkmoney.fistful.withdrawal.TimestampedChange;
import com.rbkmoney.fistful.withdrawal.WithdrawalState;
import com.rbkmoney.fraudbusters.mg.connector.constant.WithdrawalEventType;
import com.rbkmoney.fraudbusters.mg.connector.converter.FistfulAccountToDomainAccountConverter;
import com.rbkmoney.fraudbusters.mg.connector.converter.FistfulCashToDomainCashConverter;
import com.rbkmoney.fraudbusters.mg.connector.converter.FistfulResourceToDomainResourceConverter;
import com.rbkmoney.fraudbusters.mg.connector.mapper.Mapper;
import com.rbkmoney.fraudbusters.mg.connector.service.DestinationClientService;
import com.rbkmoney.fraudbusters.mg.connector.service.WithdrawalClientService;
import com.rbkmoney.fraudbusters.mg.connector.service.WalletClientService;
import com.rbkmoney.fraudbusters.mg.connector.utils.WithdrawalModelUtil;
import com.rbkmoney.geck.common.util.TBaseUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalMapper implements Mapper<TimestampedChange, MachineEvent, Withdrawal> {

    private final WithdrawalClientService withdrawalClientService;
    private final DestinationClientService destinationClientService;
    private final WalletClientService walletClientService;
    private final FistfulResourceToDomainResourceConverter fistfulResourceToDomainResourceConverter;
    private final FistfulAccountToDomainAccountConverter fistfulAccountToDomainAccountConverter;
    private final FistfulCashToDomainCashConverter fistfulCashToDomainCashConverter;

    @Override
    public boolean accept(TimestampedChange change) {
        return change.getChange().isSetStatusChanged()
                && change.getChange().getStatusChanged().isSetStatus()
                && (change.getChange().getStatusChanged().getStatus().isSetFailed()
                || change.getChange().getStatusChanged().getStatus().isSetSucceeded());
    }

    @Override
    public Withdrawal map(TimestampedChange change, MachineEvent event) {
        log.debug("Withdrawal map from change: {} event: {} ", change, event);
        Withdrawal withdrawal = new Withdrawal();
        final WithdrawalState withdrawalInfo = withdrawalClientService.getWithdrawalInfoFromFistful(
                event.getSourceId(), event.getEventId());
        withdrawalInfo.getDestinationId();
        withdrawal.setCost(fistfulCashToDomainCashConverter.convert(withdrawalInfo.getBody()));
        withdrawal.setEventTime(event.getCreatedAt());
        withdrawal.setId(event.getSourceId());
        withdrawal.setStatus(TBaseUtil.unionFieldToEnum(
                change.getChange().getStatusChanged().getStatus(),
                WithdrawalStatus.class));

        final DestinationState destinationInfo = destinationClientService.getDestinationInfoFromFistful(
                withdrawalInfo.getDestinationId());
        final WalletState walletInfoFromFistful = walletClientService.getWalletInfoFromFistful(
                withdrawalInfo.getWalletId());

        withdrawal.setAccount(fistfulAccountToDomainAccountConverter.convert(walletInfoFromFistful.getAccount()));

        final Resource resource = fistfulResourceToDomainResourceConverter.convert(destinationInfo.getResource());
        withdrawal.setDestinationResource(resource);
        withdrawal.setProviderInfo(WithdrawalModelUtil.initProviderInfo(withdrawalInfo, destinationInfo));
        withdrawal.setError(WithdrawalModelUtil.initError(change.getChange().getStatusChanged()));

        log.debug("Withdrawal map result: {}", withdrawal);
        return withdrawal;
    }

    @Override
    public WithdrawalEventType getChangeType() {
        return WithdrawalEventType.WITHDRAWAL_PAYMENT_CHARGEBACK_STATUS_CHANGED;
    }


}
