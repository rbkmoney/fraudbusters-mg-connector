package com.rbkmoney.fraudbusters.mg.connector.listener;

import com.rbkmoney.fraudbusters.mg.connector.pool.EventSinkStreamsPool;
import com.rbkmoney.fraudbusters.mg.connector.utils.ShutdownManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamStateManager {

    public static final int FATAL_ERROR_CODE_IN_STREAM = 228;

    private final EventSinkStreamsPool eventSinkStreamsPool;
    private final ShutdownManager shutdownManager;

    @Scheduled(fixedRate = 60000)
    public void monitorStateOfStreams() {
        try {
            eventSinkStreamsPool.restartAllIfShutdown();
        } catch (Exception e) {
            log.error("Error in monitor shutdown streams. {}", eventSinkStreamsPool, e);
            shutdownManager.initiateShutdown(FATAL_ERROR_CODE_IN_STREAM);
        }
    }

}
