package lk.swiftdrop.pricing.zone.controller;

import jakarta.validation.Valid;
import lk.swiftdrop.pricing.zone.dto.AdminZoneResponse;
import lk.swiftdrop.pricing.zone.dto.CreateZoneRequest;
import lk.swiftdrop.pricing.zone.dto.UpdateZoneRequest;
import lk.swiftdrop.pricing.zone.service.ZoneAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Zone management, admins only. {@code DELETE} sets the zone inactive (see {@link ZoneAdminService}). */
@RestController
@RequestMapping("/api/v1/admin/zones")
@PreAuthorize("hasRole('ADMIN')")
public class AdminZoneController {

    private final ZoneAdminService zoneAdminService;

    public AdminZoneController(ZoneAdminService zoneAdminService) {
        this.zoneAdminService = zoneAdminService;
    }

    /** All zones, inactive ones included. */
    @GetMapping
    public List<AdminZoneResponse> list() {
        return zoneAdminService.allZones();
    }

    @GetMapping("/{id}")
    public AdminZoneResponse get(@PathVariable UUID id) {
        return zoneAdminService.zone(id);
    }

    @PostMapping
    public ResponseEntity<AdminZoneResponse> create(@Valid @RequestBody CreateZoneRequest request) {
        AdminZoneResponse created = zoneAdminService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/zones/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AdminZoneResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateZoneRequest request) {
        return zoneAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        zoneAdminService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
