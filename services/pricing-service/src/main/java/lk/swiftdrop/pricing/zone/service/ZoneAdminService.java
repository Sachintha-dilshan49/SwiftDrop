package lk.swiftdrop.pricing.zone.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.pricing.zone.dto.AdminZoneResponse;
import lk.swiftdrop.pricing.zone.dto.CreateZoneRequest;
import lk.swiftdrop.pricing.zone.dto.UpdateZoneRequest;
import lk.swiftdrop.pricing.zone.entity.Zone;
import lk.swiftdrop.pricing.zone.mapper.ZoneMapper;
import lk.swiftdrop.pricing.zone.repository.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Zone management for admins. Zones are never deleted: parcel-service keeps zone codes with its parcels, so
 * "delete" sets the zone inactive and its code is never reused.
 */
@Service
public class ZoneAdminService {

    private static final Logger log = LoggerFactory.getLogger(ZoneAdminService.class);

    private final ZoneRepository zoneRepository;

    public ZoneAdminService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminZoneResponse> allZones() {
        return zoneRepository.findAllBy().stream()
                .sorted(ZoneService.DISPLAY_ORDER)
                .map(ZoneMapper::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminZoneResponse zone(UUID id) {
        return ZoneMapper.toAdminResponse(find(id));
    }

    @Transactional
    public AdminZoneResponse create(CreateZoneRequest request) {
        if (zoneRepository.existsByCode(request.code())) {
            throw new ConflictException("Zone code " + request.code() + " already exists.");
        }
        Zone zone = new Zone(request.code(), request.name().trim(), request.group(),
                request.active() == null || request.active(), cleanDistricts(request.districts()));
        zoneRepository.saveAndFlush(zone);
        log.info("Zone {} created", zone.getCode());
        return ZoneMapper.toAdminResponse(zone);
    }

    @Transactional
    public AdminZoneResponse update(UUID id, UpdateZoneRequest request) {
        Zone zone = find(id);
        zone.update(request.name().trim(), request.group(), request.active(), cleanDistricts(request.districts()));
        zoneRepository.flush();
        log.info("Zone {} updated, active={}", zone.getCode(), zone.isActive());
        return ZoneMapper.toAdminResponse(zone);
    }

    /** Sets the zone inactive; it stays in the database so its code is never reused. */
    @Transactional
    public void deactivate(UUID id) {
        Zone zone = find(id);
        zone.deactivate();
        log.info("Zone {} deactivated", zone.getCode());
    }

    private Zone find(UUID id) {
        return zoneRepository.findWithDistrictsById(id)
                .orElseThrow(() -> new NotFoundException("Zone " + id + " was not found."));
    }

    /** Trimmed, without duplicates, in the order given. */
    private static List<String> cleanDistricts(List<String> districts) {
        return districts.stream().map(String::trim).distinct().toList();
    }
}
