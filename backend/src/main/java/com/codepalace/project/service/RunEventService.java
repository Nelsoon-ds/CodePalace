package com.codepalace.project.service;


import com.codepalace.project.model.dto.client.RunResponseDto;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Streams run status changes to the browser over server-sent events.
 *
 * <p>Every subscription polls the run once per second and stops as soon as the run reaches a
 * terminal status, so a finished run never keeps a task (and a database query) alive.
 */
@Service
public class RunEventService {

    private static final Set<String> TERMINAL = Set.of(
            "COMPLETED", "FAILED", "TIMED_OUT", "OOM_KILLED", "INTERNAL_ERROR");

    private static final long POLL_INTERVAL_MS = 1_000L;
    private static final long INITIAL_DELAY_MS = 50L;

    private final RunService runService;
    private final ScheduledExecutorService scheduler;
    private final long emitterTimeoutMs;

    public RunEventService(RunService runService,
                           @Value("${app.sse.pool-size:8}") int poolSize,
                           @Value("${app.sse.timeout-ms:600000}") long emitterTimeoutMs) {
        this.runService = runService;
        this.scheduler = Executors.newScheduledThreadPool(poolSize);
        this.emitterTimeoutMs = emitterTimeoutMs;
    }

    public SseEmitter stream(UUID runId) {
        // Fails with 404 when the run does not exist or does not belong to the caller.
        runService.getOwned(runId);

        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        RunPoller poller = new RunPoller(runId, emitter);

        emitter.onCompletion(poller::stop);
        emitter.onTimeout(poller::stop);
        emitter.onError(error -> poller.stop());

        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                poller, INITIAL_DELAY_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        poller.setFuture(future);

        return emitter;
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Polls one run and pushes status changes to a single subscriber.
     */
    private final class RunPoller implements Runnable {

        private final UUID runId;
        private final SseEmitter emitter;
        private final AtomicReference<String> lastStatus = new AtomicReference<>();
        private volatile ScheduledFuture<?> future;
        private volatile boolean stopped;

        private RunPoller(UUID runId, SseEmitter emitter) {
            this.runId = runId;
            this.emitter = emitter;
        }

        void setFuture(ScheduledFuture<?> future) {
            this.future = future;
            if (stopped) {
                future.cancel(false);
            }
        }

        void stop() {
            stopped = true;
            ScheduledFuture<?> scheduled = future;
            if (scheduled != null) {
                scheduled.cancel(false);
            }
        }

        @Override
        public void run() {
            if (stopped) {
                return;
            }
            try {
                RunResponseDto run = runService.getRun(runId);
                String status = String.valueOf(run.status());

                if (!status.equals(lastStatus.get())) {
                    lastStatus.set(status);
                    emitter.send(SseEmitter.event().name("status").data(run));
                }

                if (TERMINAL.contains(status)) {
                    stop();
                    emitter.complete();
                }
            } catch (Exception e) {
                stop();
                emitter.completeWithError(e);
            }
        }
    }
}
