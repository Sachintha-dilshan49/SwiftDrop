package lk.swiftdrop.pricing.zone.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.pricing.zone.dto.AdminZoneResponse;
import lk.swiftdrop.pricing.zone.dto.CreateZoneRequest;
import lk.swiftdrop.pricing.zone.dto.UpdateZoneRequest;
import lk.swiftdrop.pricing.zone.entity.Zone;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;
import lk.swiftdrop.pricing.zone.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ZoneAdminServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private ZoneAdminService service;

    @Test
    void createsAnActiveZoneWithTrimmedDistinctDistricts() {
        given(zoneRepository.existsByCode("MATARA")).willReturn(false);

        AdminZoneResponse created = service.create(new CreateZoneRequest("MATARA", " Matara ", ZoneGroup.INTER_CITY,
                null, List.of(" Matara", "Weligama", "Matara ")));

        assertThat(created.code()).isEqualTo("MATARA");
        assertThat(created.name()).isEqualTo("Matara");
        assertThat(created.active()).isTrue();
        assertThat(created.districts()).containsExactly("Matara", "Weligama");
        verify(zoneRepository).saveAndFlush(any(Zone.class));
    }

    @Test
    void createsAnInactiveZoneWhenAsked() {
        AdminZoneResponse created = service.create(new CreateZoneRequest("MATARA", "Matara", ZoneGroup.INTER_CITY,
                false, List.of("Matara")));

        assertThat(created.active()).isFalse();
    }

    @Test
    void duplicateCodeIsAConflict() {
        given(zoneRepository.existsByCode("KANDY")).willReturn(true);

        assertThatThrownBy(() -> service.create(new CreateZoneRequest("KANDY", "Kandy", ZoneGroup.INTER_CITY, true,
                List.of("Kandy"))))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Zone code KANDY already exists.");
        verify(zoneRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesEverythingButTheCode() {
        Zone galle = new Zone("GALLE", "Galle", ZoneGroup.INTER_CITY, true, List.of("Galle"));
        given(zoneRepository.findWithDistrictsById(galle.getId())).willReturn(Optional.of(galle));

        AdminZoneResponse updated = service.update(galle.getId(), new UpdateZoneRequest("Galle Fort",
                ZoneGroup.SUBURBS, false, List.of("Galle", "Unawatuna")));

        assertThat(updated.code()).isEqualTo("GALLE");
        assertThat(updated.name()).isEqualTo("Galle Fort");
        assertThat(updated.group()).isEqualTo(ZoneGroup.SUBURBS);
        assertThat(updated.active()).isFalse();
        assertThat(updated.districts()).containsExactly("Galle", "Unawatuna");
    }

    @Test
    void deleteOnlyDeactivatesTheZone() {
        Zone galle = new Zone("GALLE", "Galle", ZoneGroup.INTER_CITY, true, List.of("Galle"));
        given(zoneRepository.findWithDistrictsById(galle.getId())).willReturn(Optional.of(galle));

        service.deactivate(galle.getId());

        assertThat(galle.isActive()).isFalse();
        verify(zoneRepository, never()).delete(any());
    }

    @Test
    void unknownZoneIsNotFound() {
        UUID id = UUID.randomUUID();
        given(zoneRepository.findWithDistrictsById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.zone(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsAllZonesInDisplayOrder() {
        given(zoneRepository.findAllBy()).willReturn(List.of(
                new Zone("KANDY", "Kandy", ZoneGroup.INTER_CITY, false, List.of("Kandy")),
                new Zone("COLOMBO_SOUTH", "Colombo South", ZoneGroup.COLOMBO, true, List.of("Colombo 04"))));

        assertThat(service.allZones()).extracting(AdminZoneResponse::code).containsExactly("COLOMBO_SOUTH", "KANDY");
    }
}
