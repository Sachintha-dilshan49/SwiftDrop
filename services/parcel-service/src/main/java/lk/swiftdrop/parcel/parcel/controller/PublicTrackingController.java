package lk.swiftdrop.parcel.parcel.controller;

import lk.swiftdrop.parcel.parcel.dto.PublicTrackingResponse;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import lk.swiftdrop.parcel.parcel.service.TrackingNumbers;
import lk.swiftdrop.parcel.tracking.service.TrackingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Public tracking by tracking number, without login ({@code swiftdrop.security.public-paths} in
 * application.yml). Input is trimmed and upper-cased; a malformed tracking number gets 400 before any
 * lookup and an unknown one gets 404.
 */
@RestController
@RequestMapping("/api/v1/track")
public class PublicTrackingController {

    private final ParcelService parcelService;
    private final TrackingService trackingService;

    public PublicTrackingController(ParcelService parcelService, TrackingService trackingService) {
        this.parcelService = parcelService;
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingNo}")
    public ResponseEntity<PublicTrackingResponse> getPublicTracking(@PathVariable String trackingNo) {
        return ResponseEntity.ok(parcelService.getPublicTracking(TrackingNumbers.normalize(trackingNo)));
    }

    /**
     * Live updates for one parcel (diagram seq-live-tracking). The 400 and 404 checks run before a stream
     * is opened; see {@link TrackingService} for the events, the limits (429) and when the stream closes.
     */
    @GetMapping(value = "/{trackingNo}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTrackingUpdates(@PathVariable String trackingNo) {
        PublicTrackingResponse snapshot = parcelService.getPublicTracking(TrackingNumbers.normalize(trackingNo));
        return trackingService.subscribe(snapshot);
    }
}
