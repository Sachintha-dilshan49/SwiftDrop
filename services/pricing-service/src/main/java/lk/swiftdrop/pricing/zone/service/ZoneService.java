package lk.swiftdrop.pricing.zone.service;

import lk.swiftdrop.pricing.zone.dto.ZoneResponse;
import lk.swiftdrop.pricing.zone.entity.Zone;
import lk.swiftdrop.pricing.zone.mapper.ZoneMapper;
import lk.swiftdrop.pricing.zone.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** The zones customers can book between. */
@Service
public class ZoneService {

    /** Colombo first, then the suburbs, then inter-city; by name within a group. */
    static final Comparator<Zone> DISPLAY_ORDER = Comparator.comparing(Zone::getGroup).thenComparing(Zone::getName);

    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> activeZones() {
        return zoneRepository.findByActiveTrue().stream()
                .sorted(DISPLAY_ORDER)
                .map(ZoneMapper::toResponse)
                .toList();
    }
}
