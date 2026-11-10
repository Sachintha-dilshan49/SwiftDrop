package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.parcel.parcel.repository.ParcelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingNumberGeneratorTest {

    @Mock
    private ParcelRepository parcelRepository;

    @InjectMocks
    private TrackingNumberGenerator generator;

    @Test
    @DisplayName("Generates tracking number matching SD-XXXX-XXXX format")
    void generatesValidFormat() {
        when(parcelRepository.existsByTrackingNo(anyString())).thenReturn(false);

        String trackingNo = generator.generateUniqueTrackingNumber();

        assertThat(trackingNo).matches("^SD-[2-9A-Z]{4}-[2-9A-Z]{4}$");
    }

    @Test
    @DisplayName("Generated tracking numbers always pass the public format check and the database constraint")
    void generatedNumbersMatchPublicFormat() {
        when(parcelRepository.existsByTrackingNo(anyString())).thenReturn(false);

        for (int i = 0; i < 1000; i++) {
            String trackingNo = generator.generateUniqueTrackingNumber();
            assertThat(TrackingNumbers.FORMAT.matcher(trackingNo).matches()).as(trackingNo).isTrue();
            assertThat(TrackingNumbers.normalize(trackingNo)).isEqualTo(trackingNo);
        }
    }

    @Test
    @DisplayName("Retries generation if tracking number already exists")
    void retriesOnCollision() {
        when(parcelRepository.existsByTrackingNo(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String trackingNo = generator.generateUniqueTrackingNumber();

        assertThat(trackingNo).isNotNull();
    }
}
