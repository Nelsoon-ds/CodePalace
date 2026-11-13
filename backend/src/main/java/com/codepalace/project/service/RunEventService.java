package com.codepalace.project.service;


import com.codepalace.project.model.dto.client.RunResponseDto;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RunEventService {

    private static final Set<String> TERMINAL = Set.of(
            "COMPLETED", "FAILED", "TIMED_OUT", "OOM_KILLED", "INTERNAL_ERROR");

    private final RunService runService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public RunEventService(RunService runService) {
        this.runService = runService;
    }

    public SseEmitter stream(UUID runId) {
        runService.getRun(runId); // 404 up front if the run doesn't exist

        SseEmitter emitter = new SseEmitter(5 * 60_000L);
        AtomicReference<String> lastStatus = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();

        Runnable stop = () -> {
            ScheduledFuture<?> f = task.get();
            if (f != null) f.cancel(false);
        };
        emitter.onCompletion(stop);
        emitter.onTimeout(stop);
        emitter.onError(e -> stop.run());

        Runnable poll = () -> {
            try {
                RunResponseDto run = runService.getRun(runId);
                String status = String.valueOf(run.status());
                if (!status.equals(lastStatus.get())) {
                    lastStatus.set(status);
                    emitter.send(SseEmitter.event().name("status").data(run));
                }
                if (TERMINAL.contains(status)) {
                    emitter.complete();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        };

        task.set(scheduler.scheduleWithFixedDelay(poll, 0, 1, TimeUnit.SECONDS));
        return emitter;
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
    }
}