package lk.swiftdrop.parcel.parcel.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.dto.CreateParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.PageResponse;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.ParcelSummaryResponse;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parcels")
public class CustomerParcelController {

    private final ParcelService parcelService;

    public CustomerParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ParcelResponse> createParcel(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateParcelRequest request) {
        UUID customerId = JwtCaller.userId(jwt);
        ParcelResponse response = parcelService.createCustomerParcel(customerId, request);
        return ResponseEntity.created(URI.create("/api/v1/parcels/" + response.id())).body(response);
    }

    /**
     * The caller's own parcels, or every parcel for dispatchers and admins, newest first by default.
     * Paged as in docs/conventions.md §1; {@code size} is capped at 100.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ParcelSummaryResponse>> getMyParcels(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ParcelStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "must be 0 or greater") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "must be 1 or greater") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        UUID callerId = JwtCaller.userId(jwt);
        return ResponseEntity.ok(parcelService.listParcels(
                callerId, JwtCaller.isPrivileged(jwt), status, ParcelPageRequests.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ParcelResponse> getParcel(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID customerId = JwtCaller.userId(jwt);
        return ResponseEntity.ok(parcelService.getParcelForCustomer(id, customerId, JwtCaller.isPrivileged(jwt)));
    }
}
