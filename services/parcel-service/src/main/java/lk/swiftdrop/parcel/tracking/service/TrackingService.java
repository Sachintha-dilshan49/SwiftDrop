package lk.swiftdrop.parcel.tracking.service;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.parcel.dto.PublicTrackingResponse;
import lk.swiftdrop.parcel.tracking.config.TrackingSseProperties;
import lk.swiftdrop.parcel.tracking.dto.PublicTrackingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Live tracking streams (Server-Sent Events, docs/architecture.md §6).
 *
 * <ul>
 *   <li>Each stream starts with a {@value #SNAPSHOT_EVENT} event holding the current public tracking view,
 *       then gets a {@value #STATUS_CHANGE_EVENT} event per status change, whose SSE id is the event's
 *       {@code eventId}. A heartbeat comment keeps proxies from closing idle streams.</li>
 *   <li>Missed events are not replayed: a reconnecting browser sends {@code Last-Event-ID}, but it is
 *       ignored and the client gets a fresh snapshot instead.</li>
 *   <li>Streams close after DELIVERED or FAILED. Open streams are limited per tracking number and per
 *       instance; above the limit, subscribing fails with 429.</li>
 * </ul>
 */
@Service
public class TrackingService {

    static final String SNAPSHOT_EVENT = "SNAPSHOT";
    static final String STATUS_CHANGE_EVENT = "STATUS_CHANGE";

    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);

    private final TrackingSseProperties properties;
    private final Map<String, List<SseEmitter>> emittersByTrackingNo = new ConcurrentHashMap<>();
    private final AtomicInteger openEmitters = new AtomicInteger();

    public TrackingService(TrackingSseProperties properties) {
        this.properties = properties;
    }

    /**
     * Opens a stream for a parcel that exists, and sends its current state first.
     *
     * @throws TooManySubscribersException when a connection limit is reached
     */
    public SseEmitter subscribe(PublicTrackingResponse snapshot) {
        String trackingNo = snapshot.trackingNo();
        SseEmitter emitter = newEmitter(properties.timeout().toMillis());
        register(trackingNo, emitter);

        Runnable cleanup = () -> removeEmitter(trackingNo, emitter);
        emitter.onCompletion(cleanup);
        // Completing on timeout ends the stream normally (the browser reconnects and gets a new snapshot);
        // otherwise Spring raises AsyncRequestTimeoutException on an already started event stream.
        emitter.onTimeout(() -> close(trackingNo, emitter));
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name(SNAPSHOT_EVENT).data(snapshot));
            if (isTerminal(snapshot.status())) {
                close(trackingNo, emitter);
            }
        } catch (Exception e) {
            log.debug("Could not send tracking snapshot for {}: {}", trackingNo, e.getMessage());
            removeEmitter(trackingNo, emitter);
        }
        return emitter;
    }

    /** Pushes a status change to every open stream of that parcel, with public fields only. */
    public void sendUpdate(ParcelStatusChanged event) {
        String trackingNo = event.trackingNo();
        List<SseEmitter> emitters = emittersByTrackingNo.get(trackingNo);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        PublicTrackingEvent update = PublicTrackingEvent.from(event);
        boolean terminal = isTerminal(event.toStatus());
        log.debug("Broadcasting tracking update {} to {} active SSE listeners", trackingNo, emitters.size());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventId().toString())
                        .name(STATUS_CHANGE_EVENT)
                        .data(update));
                if (terminal) {
                    close(trackingNo, emitter);
                }
            } catch (Exception e) {
                log.debug("Removing dead SSE emitter for {}: {}", trackingNo, e.getMessage());
                removeEmitter(trackingNo, emitter);
            }
        }
    }

    @Scheduled(fixedRateString = "${swiftdrop.tracking.sse.heartbeat-interval-ms:20000}")
    public void sendHeartbeat() {
        for (Map.Entry<String, List<SseEmitter>> entry : emittersByTrackingNo.entrySet()) {
            String trackingNo = entry.getKey();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception e) {
                    removeEmitter(trackingNo, emitter);
                }
            }
        }
    }

    /** Number of open streams on this instance. */
    int openEmitterCount() {
        return openEmitters.get();
    }

    /** Separate method so tests can observe what is sent. */
    SseEmitter newEmitter(long timeoutMillis) {
        return new SseEmitter(timeoutMillis);
    }

    private void register(String trackingNo, SseEmitter emitter) {
        if (openEmitters.incrementAndGet() > properties.maxTotal()) {
            openEmitters.decrementAndGet();
            throw new TooManySubscribersException();
        }
        AtomicBoolean added = new AtomicBoolean();
        emittersByTrackingNo.compute(trackingNo, (key, existing) -> {
            List<SseEmitter> emitters = existing != null ? existing : new CopyOnWriteArrayList<>();
            if (emitters.size() < properties.maxPerTrackingNo()) {
                emitters.add(emitter);
                added.set(true);
            }
            return emitters.isEmpty() ? null : emitters;
        });
        if (!added.get()) {
            openEmitters.decrementAndGet();
            throw new TooManySubscribersException();
        }
    }

    private void close(String trackingNo, SseEmitter emitter) {
        removeEmitter(trackingNo, emitter);
        emitter.complete();
    }

    private void removeEmitter(String trackingNo, SseEmitter emitter) {
        emittersByTrackingNo.computeIfPresent(trackingNo, (key, emitters) -> {
            if (emitters.remove(emitter)) {
                openEmitters.decrementAndGet();
            }
            return emitters.isEmpty() ? null : emitters;
        });
    }

    private static boolean isTerminal(ParcelStatus status) {
        return status == ParcelStatus.DELIVERED || status == ParcelStatus.FAILED;
    }
}
