package lk.swiftdrop.dispatch.parcel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import lk.swiftdrop.common.event.ParcelStatus;
import org.junit.jupiter.api.Test;

class ParcelStatusUpdateTest {

    private static final UUID COURIER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void courierUpdateRecordsWhoMadeTheChange() {
        ParcelStatusUpdate update = ParcelStatusUpdate.byCourier(
                ParcelStatus.FAILED, COURIER_ID, USER_ID, "Gate locked", null, "Nobody at home", "proof-key");

        assertThat(update).isEqualTo(new ParcelStatusUpdate(ParcelStatus.FAILED, COURIER_ID, "Gate locked",
                "proof-key", "Nobody at home", USER_ID, "COURIER"));
    }

    @Test
    void recipientNameGoesIntoTheNote() {
        assertThat(note(null, " Nimali ")).isEqualTo("Received by: Nimali");
        assertThat(note("At the gate", "Nimali")).isEqualTo("Received by: Nimali. At the gate");
        assertThat(note("At the gate", " ")).isEqualTo("At the gate");
        assertThat(note(" ", null)).isNull();
    }

    @Test
    void noteNeverExceedsWhatParcelServiceAccepts() {
        assertThat(note("x".repeat(500), "y".repeat(150))).hasSize(ParcelStatusUpdate.MAX_NOTE_LENGTH);
    }

    private static String note(String note, String recipientName) {
        return ParcelStatusUpdate.byCourier(ParcelStatus.DELIVERED, COURIER_ID, USER_ID, note, recipientName, null, null)
                .note();
    }
}
