package lk.swiftdrop.parcel.parcel.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.parcel.parcel.dto.CreateInternalParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.CreateInternalParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.RecipientContactsResponse;
import lk.swiftdrop.parcel.parcel.dto.UpdateParcelStatusRequest;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Service-to-service endpoints, never routed by the gateway. {@code common} lets a request in only
 * with the shared {@code X-Internal-Token} (ADR-010); a user JWT alone gets 401.
 */
@RestController
@RequestMapping("/internal/parcels")
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalParcelController {

    private final ParcelService parcelService;

    public InternalParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    /** A merchant order from merchant-service; the fee comes from pricing-service, never from the request. */
    @PostMapping
    public ResponseEntity<CreateInternalParcelResponse> createMerchantParcel(
            @Valid @RequestBody CreateInternalParcelRequest request) {
        CreateInternalParcelResponse response = parcelService.createMerchantParcel(request);
        return ResponseEntity.created(URI.create("/internal/parcels/" + response.id())).body(response);
    }

    /** The recipient's contacts for notification-service, fetched when it sends a message (events carry IDs only). */
    @GetMapping("/{id}/contacts")
    public ResponseEntity<RecipientContactsResponse> getRecipientContacts(@PathVariable UUID id) {
        return ResponseEntity.ok(parcelService.getRecipientContacts(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ParcelResponse> updateParcelStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParcelStatusRequest request) {
        ParcelResponse response = parcelService.updateParcelStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
