package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackingNumbersTest {

    @Test
    @DisplayName("Accepts a well-formed tracking number unchanged")
    void acceptsWellFormedNumber() {
        assertThat(TrackingNumbers.normalize("SD-7K3Q-9P2M")).isEqualTo("SD-7K3Q-9P2M");
    }

    @Test
    @DisplayName("Trims and upper-cases user input")
    void normalizesCaseAndWhitespace() {
        assertThat(TrackingNumbers.normalize("  sd-7k3q-9p2m ")).isEqualTo("SD-7K3Q-9P2M");
    }

    @ParameterizedTest(name = "Rejects \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {
            "SD-UNKNOWN",      // wrong shape
            "SD-7K3Q-9P2",     // too short
            "SD-7K3Q-9P2MM",   // too long
            "XX-7K3Q-9P2M",    // wrong prefix
            "SD-7K3O-9P2M",    // O is not used
            "SD-7K3I-9P2M",    // I is not used
            "SD-0K3Q-9P2M",    // 0 is not used
            "SD-1K3Q-9P2M",    // 1 is not used
            "SD7K3Q9P2M",      // no dashes
            "SD-7K3Q-9P2M/../x"
    })
    void rejectsMalformedNumbers(String input) {
        assertThatThrownBy(() -> TrackingNumbers.normalize(input))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).getErrors())
                        .extracting("field").containsExactly("trackingNo"));
    }
}
