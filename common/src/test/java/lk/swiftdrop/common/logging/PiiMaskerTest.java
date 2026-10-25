package lk.swiftdrop.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PiiMaskerTest {

    @Test
    void masksEmailAddresses() {
        assertThat(PiiMasker.mask("Sent receipt to jane.doe+parcels@example.co.uk today"))
                .isEqualTo("Sent receipt to [EMAIL] today");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0771234567", "077 123 4567", "077-123-4567", "+94771234567", "+94 77 123 4567",
            "0094771234567", "011 234 5678", "+44 20 7946 0958"})
    void masksPhoneNumbers(String phone) {
        assertThat(PiiMasker.mask("call " + phone + " now")).isEqualTo("call [PHONE] now");
    }

    @Test
    void masksAddressValuesInToStringAndJson() {
        assertThat(PiiMasker.mask("Parcel[id=42, deliveryAddress=12 Temple Rd, Kandy, weightKg=2.5]"))
                .isEqualTo("Parcel[id=42, deliveryAddress=[ADDRESS], weightKg=2.5]");
        assertThat(PiiMasker.mask("{\"pickupAddress\":\"7 \\\"Lake\\\" View, Colombo 03\",\"zone\":\"WEST\"}"))
                .isEqualTo("{\"pickupAddress\":\"[ADDRESS]\",\"zone\":\"WEST\"}");
        assertThat(PiiMasker.mask("street: 5th Lane")).isEqualTo("street: [ADDRESS]");
    }

    @Test
    void masksBearerTokensAndJwts() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjMifQ.c2lnbmF0dXJl";
        assertThat(PiiMasker.mask("Authorization: Bearer " + jwt)).isEqualTo("Authorization: Bearer [TOKEN]");
        assertThat(PiiMasker.mask("token=" + jwt)).isEqualTo("token=[TOKEN]");
    }

    @Test
    void leavesIdsTimestampsTrackingNumbersAndAmountsAlone() {
        String text = "Parcel 8f2c1e0a-3b5d-4c2e-9a71-077123456789 SD-7K3Q-9P2M moved to OUT_FOR_DELIVERY"
                + " at 2026-10-21T09:15:00Z, fee 1250.00 LKR, address changed by courier 12";
        assertThat(PiiMasker.mask(text)).isEqualTo(text);
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(PiiMasker.mask(null)).isNull();
        assertThat(PiiMasker.mask("")).isEmpty();
    }
}
