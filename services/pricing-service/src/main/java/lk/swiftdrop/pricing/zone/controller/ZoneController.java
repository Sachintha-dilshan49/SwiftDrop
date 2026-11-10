package lk.swiftdrop.pricing.zone.controller;

import lk.swiftdrop.pricing.zone.dto.ZoneResponse;
import lk.swiftdrop.pricing.zone.service.ZoneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The active delivery zones, for the booking form and guests. Public (no token). */
@RestController
@RequestMapping("/api/v1/pricing")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping("/zones")
    public List<ZoneResponse> zones() {
        return zoneService.activeZones();
    }
}
