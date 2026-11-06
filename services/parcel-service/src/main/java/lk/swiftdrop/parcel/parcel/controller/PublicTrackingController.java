package lk.swiftdrop.parcel.parcel.controller;

import lk.swiftdrop.parcel.parcel.dto.PublicTrackingResponse;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import lk.swiftdrop.parcel.parcel.service.TrackingNumbers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public tracking by tracking number, without login ({@code swiftdrop.security.public-paths} in
 * application.yml). Input is trimmed and upper-cased; a malformed tracking number gets 400 before any
 * lookup and an unknown one gets 404.
 */
@RestController
@RequestMapping("/api/v1/track")
public class PublicTrackingController {

    private final ParcelService parcelService;

    public PublicTrackingController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @GetMapping("/{trackingNo}")
    public ResponseEntity<PublicTrackingResponse> getPublicTracking(@PathVariable String trackingNo) {
        return ResponseEntity.ok(parcelService.getPublicTracking(TrackingNumbers.normalize(trackingNo)));
    }
}
