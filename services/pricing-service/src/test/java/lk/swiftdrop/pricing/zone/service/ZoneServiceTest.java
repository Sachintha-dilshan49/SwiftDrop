package lk.swiftdrop.pricing.zone.service;

import lk.swiftdrop.pricing.zone.dto.ZoneResponse;
import lk.swiftdrop.pricing.zone.entity.Zone;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;
import lk.swiftdrop.pricing.zone.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private ZoneService zoneService;

    @Test
    void listsActiveZonesColomboFirstThenSuburbsThenInterCityByName() {
        given(zoneRepository.findByActiveTrue()).willReturn(List.of(
                new Zone("KANDY", "Kandy", ZoneGroup.INTER_CITY, true, List.of("Kandy")),
                new Zone("NUGEGODA_MAHARAGAMA", "Nugegoda & Maharagama", ZoneGroup.SUBURBS, true,
                        List.of("Nugegoda", "Maharagama")),
                new Zone("COLOMBO_SOUTH", "Colombo South", ZoneGroup.COLOMBO, true, List.of("Colombo 04")),
                new Zone("GALLE", "Galle", ZoneGroup.INTER_CITY, true, List.of("Galle")),
                new Zone("COLOMBO_CENTRAL", "Colombo Central", ZoneGroup.COLOMBO, true, List.of("Colombo 01"))));

        List<ZoneResponse> zones = zoneService.activeZones();

        assertThat(zones).extracting(ZoneResponse::code).containsExactly(
                "COLOMBO_CENTRAL", "COLOMBO_SOUTH", "NUGEGODA_MAHARAGAMA", "GALLE", "KANDY");
        assertThat(zones.get(2).districts()).containsExactly("Nugegoda", "Maharagama");
    }
}
