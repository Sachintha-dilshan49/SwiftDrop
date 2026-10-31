package lk.swiftdrop.parcel.parcel.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.parcel.parcel.dto.CreateParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.service.ParcelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
