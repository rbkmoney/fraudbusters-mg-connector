package com.rbkmoney.fraudbusters.mg.connector.utils;

import com.rbkmoney.damsel.fraudbusters.Error;
import com.rbkmoney.damsel.fraudbusters.ProviderInfo;
import com.rbkmoney.fistful.base.Failure;
import com.rbkmoney.fistful.destination.DestinationState;
import com.rbkmoney.fistful.withdrawal.StatusChange;
import com.rbkmoney.fistful.withdrawal.WithdrawalState;
import com.rbkmoney.fistful.withdrawal.status.Failed;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TDomainToStringErrorHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class WithdrawalModelUtil {

    public static ProviderInfo initProviderInfo(WithdrawalState withdrawalState, DestinationState destinationState) {
        if (!withdrawalState.isSetRoute()) {
            return null;
        }
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.setTerminalId(String.valueOf(withdrawalState.getRoute().getTerminalId()));
        providerInfo.setProviderId(String.valueOf(withdrawalState.getRoute().getProviderId()));
        if (destinationState.getResource().isSetBankCard()
                && destinationState.getResource().getBankCard().isSetBankCard()
                && destinationState.getResource().getBankCard().getBankCard().isSetIssuerCountry()) {
            providerInfo
                    .setCountry(destinationState.getResource().getBankCard().getBankCard().getIssuerCountry().name());
        }
        return providerInfo;
    }

    public static Error initError(StatusChange statusChange) {
        Error error = null;
        if (statusChange.getStatus().isSetFailed()) {
            error = new Error();
            final Failed failed = statusChange.getStatus().getFailed();
            if (failed.isSetFailure()) {
                final com.rbkmoney.fistful.base.Failure failure = failed.getFailure();
                error.setErrorCode(parseError(failure))
                        .setErrorReason(failure.getReason());

            } else {
                error.setErrorCode("unknown error");
            }
        }
        return error;
    }

    private static String parseError(Failure failure) {
        try {
            return (new TBaseProcessor()).process(failure, new TDomainToStringErrorHandler());
        } catch (IOException e) {
            log.error("Error when parse error: {}", failure);
            return failure.getCode();
        }
    }

}
