package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.entity.RouteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReceiptServiceTest {

    private final PdfReceiptService service = new PdfReceiptService("http://localhost:3000/track/");

    /** Colombo South to Kandy, 3.2 kg, MEDIUM, EXPRESS with DROP100: 650 + 120 + 100 + 100 - 100 = 870. */
    private static Parcel bookedParcel() {
        Parcel parcel = new Parcel(UUID.randomUUID(), "SD-7K3Q-9P2M", UUID.randomUUID(), null,
                "No. 12, Galle Road, Colombo 03", "No. 45, Peradeniya Road, Kandy", "Kamal Silva",
                "+94719876543", ParcelSize.MEDIUM, new BigDecimal("3.20"), null, "LKR");
        parcel.setSpeed(DeliverySpeed.EXPRESS);
        parcel.setPromoCode("DROP100");
        parcel.applyFee(RouteType.INTER_CITY, new BigDecimal("650.00"), new BigDecimal("120.00"),
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("870.00"), "LKR");
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        parcel.setCreatedAt(Instant.parse("2026-11-06T09:15:00Z"));
        return parcel;
    }

    private static String textOf(byte[] pdf) throws IOException {
        return new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
    }

    @Test
    @DisplayName("The receipt is a PDF document")
    void receiptIsAPdf() {
        byte[] pdf = service.generateReceipt(bookedParcel());

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("The receipt shows every part of the stored fee and the total")
    void receiptShowsFeeBreakdownAndTotal() throws IOException {
        String text = textOf(service.generateReceipt(bookedParcel()));

        assertThat(text).contains(
                "Fee breakdown",
                "Route base fee", "LKR 650.00",
                "Extra weight", "LKR 120.00",
                "Size surcharge", "Express surcharge", "LKR 100.00",
                "Promo discount (DROP100)", "- LKR 100.00",
                "Total", "LKR 870.00");
    }

    @Test
    @DisplayName("The receipt shows the booking details in readable form")
    void receiptShowsBookingDetails() throws IOException {
        String text = textOf(service.generateReceipt(bookedParcel()));

        assertThat(text).contains(
                "SD-7K3Q-9P2M",
                "2026-11-06 09:15 UTC",
                "Out for delivery",
                "No. 12, Galle Road, Colombo 03",
                "No. 45, Peradeniya Road, Kandy",
                "Kamal Silva",
                "Medium", "3.20 kg", "Express", "Inter city");
    }

    @Test
    @DisplayName("The receipt hides all but the last three digits of the recipient's phone number")
    void receiptMasksRecipientPhone() throws IOException {
        String text = textOf(service.generateReceipt(bookedParcel()));

        assertThat(text).contains("+********543").doesNotContain("719876543");
    }

    @Test
    @DisplayName("The receipt links to the public tracking page")
    void receiptLinksToTrackingPage() throws IOException {
        String text = textOf(service.generateReceipt(bookedParcel()));

        assertThat(text).contains("http://localhost:3000/track/SD-7K3Q-9P2M");
    }

    @Test
    @DisplayName("Without a promo code the discount line has no code and shows zero")
    void receiptWithoutPromoCode() throws IOException {
        Parcel parcel = bookedParcel();
        parcel.setPromoCode(null);
        parcel.applyFee(RouteType.SAME_ZONE, new BigDecimal("1250.00"), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                new BigDecimal("1250.00"), "LKR");

        String text = textOf(service.generateReceipt(parcel));

        assertThat(text).contains("Promo discount", "LKR 0.00", "LKR 1,250.00")
                .doesNotContain("Promo discount (", "- LKR");
    }

    @ParameterizedTest(name = "{0} is shown as {1}")
    @CsvSource({
            "+94719876543, +********543",
            "0771234567, *******567",
            "077 123 4567, *** *** *567",
            "12, **"
    })
    void maskPhoneKeepsOnlyTheLastThreeDigits(String phone, String expected) {
        assertThat(PdfReceiptService.maskPhone(phone)).isEqualTo(expected);
    }

    @Test
    @DisplayName("A missing value is shown as N/A")
    void missingValuesAreShownAsNotAvailable() {
        assertThat(PdfReceiptService.maskPhone(null)).isEqualTo("N/A");
        assertThat(PdfReceiptService.money(null, "LKR")).isEqualTo("N/A");
    }
}
