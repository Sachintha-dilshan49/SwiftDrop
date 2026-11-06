package lk.swiftdrop.parcel.tracking.service;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.parcel.parcel.dto.PublicStatusHistoryResponse;
import lk.swiftdrop.parcel.parcel.dto.PublicTrackingResponse;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.tracking.config.TrackingSseProperties;
import lk.swiftdrop.parcel.tracking.dto.PublicTrackingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackingServiceTest {

    private static final String TRACKING_NO = "SD-7K3Q-9P2M";
    private static final String OTHER_TRACKING_NO = "SD-2222-3333";

    private final List<RecordingEmitter> emitters = new ArrayList<>();
    private boolean newEmittersFail;

    private TrackingService service(int maxPerTrackingNo, int maxTotal) {
        TrackingSseProperties properties = new TrackingSseProperties(maxPerTrackingNo, maxTotal, Duration.ofMinutes(30));
        return new TrackingService(properties) {
            @Override
            SseEmitter newEmitter(long timeoutMillis) {
                RecordingEmitter emitter = new RecordingEmitter(timeoutMillis);
                emitter.failOnSend = newEmittersFail;
                emitters.add(emitter);
                return emitter;
            }
        };
    }

    @Test
    @DisplayName("A new stream gets the current public snapshot first")
    void subscribeSendsSnapshotFirst() {
        TrackingService service = service(10, 100);
        PublicTrackingResponse snapshot = snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT);

        SseEmitter emitter = service.subscribe(snapshot);

        RecordingEmitter recorded = (RecordingEmitter) emitter;
        assertThat(emitter.getTimeout()).isEqualTo(Duration.ofMinutes(30).toMillis());
        assertThat(recorded.events).hasSize(1);
        assertThat(recorded.text(0)).contains("event:SNAPSHOT");
        assertThat(recorded.payloads(0)).containsExactly(snapshot);
        assertThat(recorded.completed).isFalse();
        assertThat(service.openEmitterCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Subscribing to a delivered or failed parcel sends the snapshot and closes the stream")
    void terminalSnapshotClosesStream() {
        TrackingService service = service(10, 100);

        RecordingEmitter delivered = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.DELIVERED));
        RecordingEmitter failed = (RecordingEmitter) service.subscribe(snapshot(OTHER_TRACKING_NO, ParcelStatus.FAILED));

        assertThat(delivered.events).hasSize(1);
        assertThat(delivered.completed).isTrue();
        assertThat(failed.completed).isTrue();
        assertThat(service.openEmitterCount()).isZero();
    }

    @Test
    @DisplayName("Rejects streams beyond the per-tracking-number limit with 429; other parcels are unaffected")
    void enforcesPerTrackingNumberLimit() {
        TrackingService service = service(2, 100);
        service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));
        service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));

        assertThatThrownBy(() -> service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT)))
                .isInstanceOf(TooManySubscribersException.class);
        assertThat(service.openEmitterCount()).isEqualTo(2);

        assertThatCode(() -> service.subscribe(snapshot(OTHER_TRACKING_NO, ParcelStatus.IN_TRANSIT)))
                .doesNotThrowAnyException();
        assertThat(service.openEmitterCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Rejects streams beyond the per-instance limit with 429")
    void enforcesInstanceLimit() {
        TrackingService service = service(10, 2);
        service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));
        service.subscribe(snapshot(OTHER_TRACKING_NO, ParcelStatus.IN_TRANSIT));

        assertThatThrownBy(() -> service.subscribe(snapshot("SD-4444-5555", ParcelStatus.IN_TRANSIT)))
                .isInstanceOf(TooManySubscribersException.class);
        assertThat(service.openEmitterCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("A status change sends only public fields, with the eventId as SSE id")
    void statusChangeSendsOnlyPublicFields() {
        TrackingService service = service(10, 100);
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));
        ParcelStatusChanged event = event(TRACKING_NO, ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY);

        service.sendUpdate(event);

        assertThat(emitter.events).hasSize(2);
        assertThat(emitter.text(1)).contains("event:STATUS_CHANGE").contains("id:" + event.eventId());
        assertThat(emitter.payloads(1)).containsExactly(new PublicTrackingEvent(
                TRACKING_NO, ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.IN_TRANSIT, event.occurredAt()));
        assertThat(emitter.allPayloads()).noneMatch(ParcelStatusChanged.class::isInstance);
        assertThat(emitter.completed).isFalse();
    }

    @Test
    @DisplayName("The public SSE event carries exactly trackingNo, status, previousStatus and occurredAt")
    void publicEventHasNoPrivateFields() {
        List<String> fields = Arrays.stream(PublicTrackingEvent.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(fields).containsExactly("trackingNo", "status", "previousStatus", "occurredAt");
    }

    @Test
    @DisplayName("Updates only reach streams of the same tracking number")
    void updatesOnlyReachTheirParcel() {
        TrackingService service = service(10, 100);
        RecordingEmitter mine = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));
        RecordingEmitter other = (RecordingEmitter) service.subscribe(snapshot(OTHER_TRACKING_NO, ParcelStatus.IN_TRANSIT));

        service.sendUpdate(event(TRACKING_NO, ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY));

        assertThat(mine.events).hasSize(2);
        assertThat(other.events).hasSize(1);
    }

    @Test
    @DisplayName("A DELIVERED or FAILED update is sent and then the streams close")
    void terminalUpdateClosesStreams() {
        TrackingService service = service(10, 100);
        RecordingEmitter first = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.OUT_FOR_DELIVERY));
        RecordingEmitter second = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.OUT_FOR_DELIVERY));

        service.sendUpdate(event(TRACKING_NO, ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERED));
        service.sendUpdate(event(TRACKING_NO, ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.FAILED));

        assertThat(first.events).hasSize(2);
        assertThat(first.completed).isTrue();
        assertThat(second.completed).isTrue();
        assertThat(service.openEmitterCount()).isZero();
    }

    @Test
    @DisplayName("A stream whose client went away is removed and frees its slot")
    void removesDeadEmitterOnSend() {
        TrackingService service = service(1, 100);
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));
        emitter.failOnSend = true;

        service.sendUpdate(event(TRACKING_NO, ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY));

        assertThat(service.openEmitterCount()).isZero();
        assertThatCode(() -> service.subscribe(snapshot(TRACKING_NO, ParcelStatus.OUT_FOR_DELIVERY)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A stream that reaches its timeout is closed normally and frees its slot")
    void timedOutStreamIsClosedAndFreesSlot() {
        TrackingService service = service(1, 100);
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));

        emitter.timeoutCallback.run();

        assertThat(emitter.completed).isTrue();
        assertThat(service.openEmitterCount()).isZero();
        assertThatCode(() -> service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A stream whose snapshot cannot be sent does not keep a slot")
    void failedSnapshotDoesNotLeakSlot() {
        TrackingService service = service(1, 100);
        newEmittersFail = true;

        service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));

        assertThat(service.openEmitterCount()).isZero();
    }

    @Test
    @DisplayName("Heartbeats go to live streams and remove dead ones")
    void heartbeatKeepsLiveStreamsAndRemovesDeadOnes() {
        TrackingService service = service(10, 100);
        RecordingEmitter live = (RecordingEmitter) service.subscribe(snapshot(TRACKING_NO, ParcelStatus.IN_TRANSIT));
        RecordingEmitter dead = (RecordingEmitter) service.subscribe(snapshot(OTHER_TRACKING_NO, ParcelStatus.IN_TRANSIT));
        dead.failOnSend = true;

        service.sendHeartbeat();

        assertThat(live.text(1)).contains(":heartbeat");
        assertThat(service.openEmitterCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("An update for a parcel nobody is watching is ignored")
    void updateWithoutSubscribersIsIgnored() {
        TrackingService service = service(10, 100);

        assertThatCode(() -> service.sendUpdate(event(TRACKING_NO, ParcelStatus.IN_TRANSIT, ParcelStatus.OUT_FOR_DELIVERY)))
                .doesNotThrowAnyException();
        assertThat(emitters).isEmpty();
    }

    private static PublicTrackingResponse snapshot(String trackingNo, ParcelStatus status) {
        return new PublicTrackingResponse(trackingNo, status, ParcelSize.MEDIUM, new BigDecimal("2.50"),
                Instant.now(), List.of(new PublicStatusHistoryResponse(status, Instant.now())));
    }

    private static ParcelStatusChanged event(String trackingNo, ParcelStatus from, ParcelStatus to) {
        return ParcelStatusChanged.of(Instant.now(), UUID.randomUUID(), trackingNo, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), from, to, "Courier note with a name in it",
                to == ParcelStatus.FAILED ? "Recipient unavailable" : null);
    }

    /** Captures what would be written to the browser. */
    static class RecordingEmitter extends SseEmitter {

        final List<Set<ResponseBodyEmitter.DataWithMediaType>> events = new ArrayList<>();
        boolean failOnSend;
        boolean completed;
        Runnable timeoutCallback;

        RecordingEmitter(long timeout) {
            super(timeout);
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            timeoutCallback = callback;
            super.onTimeout(callback);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failOnSend) {
                throw new IOException("Client went away");
            }
            events.add(builder.build());
        }

        @Override
        public synchronized void complete() {
            completed = true;
            super.complete();
        }

        String text(int index) {
            StringBuilder text = new StringBuilder();
            events.get(index).stream()
                    .map(ResponseBodyEmitter.DataWithMediaType::getData)
                    .filter(String.class::isInstance)
                    .forEach(text::append);
            return text.toString();
        }

        List<Object> payloads(int index) {
            return events.get(index).stream()
                    .map(ResponseBodyEmitter.DataWithMediaType::getData)
                    .filter(data -> !(data instanceof String))
                    .toList();
        }

        List<Object> allPayloads() {
            List<Object> all = new ArrayList<>();
            for (int i = 0; i < events.size(); i++) {
                all.addAll(payloads(i));
            }
            return all;
        }
    }
}
