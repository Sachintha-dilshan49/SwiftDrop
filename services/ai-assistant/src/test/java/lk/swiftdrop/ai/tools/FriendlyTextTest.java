package lk.swiftdrop.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FriendlyTextTest {

    @Test
    void everyParcelStatusHasCustomerWordsAndAMeaning() {
        for (String code : new String[] {"CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED"}) {
            assertThat(FriendlyText.statusLabel(code)).doesNotContain("_").isNotEqualTo(code);
            assertThat(FriendlyText.statusMeaning(code)).isNotBlank();
        }
        assertThat(FriendlyText.statusLabel("CREATED")).isEqualTo("Booked");
        assertThat(FriendlyText.statusLabel("FAILED")).isEqualTo("Delivery failed");
    }

    @Test
    void unknownStatusIsTidiedUpAndMissingStatusStaysEmpty() {
        assertThat(FriendlyText.statusLabel("RETURNED_TO_SENDER")).isEqualTo("Returned to sender");
        assertThat(FriendlyText.statusMeaning("RETURNED_TO_SENDER")).isNull();
        assertThat(FriendlyText.statusLabel(null)).isNull();
    }

    @Test
    void codesBecomeWords() {
        assertThat(FriendlyText.words("EXTRA_LARGE")).isEqualTo("Extra large");
        assertThat(FriendlyText.words("NEXT_DAY")).isEqualTo("Next day");
        assertThat(FriendlyText.words("")).isNull();
    }

    @Test
    void timesAreShownInSriLankaTime() {
        assertThat(FriendlyText.time("2026-11-14T06:15:58Z")).isEqualTo("14 Nov 2026, 11:45 am");
        assertThat(FriendlyText.time("2026-11-14T18:40:00.123456Z")).isEqualTo("15 Nov 2026, 12:10 am");
    }

    @Test
    void textThatIsNotATimeIsKept() {
        assertThat(FriendlyText.time("yesterday")).isEqualTo("yesterday");
        assertThat(FriendlyText.time(" ")).isNull();
    }
}
