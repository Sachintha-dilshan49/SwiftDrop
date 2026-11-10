package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.parcel.parcel.entity.Parcel;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds the booking receipt PDF from the saved parcel (diagram seq-parcel-history-receipt).
 *
 * <p>The fee breakdown is the one stored from pricing-service's quote at booking, so a receipt
 * stays the same when the pricing rules change later (docs/architecture.md §8). The recipient's
 * phone number shows only its last {@value #VISIBLE_PHONE_DIGITS} digits.
 */
@Service
public class PdfReceiptService {

    static final int VISIBLE_PHONE_DIGITS = 3;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);
    private static final String NOT_AVAILABLE = "N/A";

    private static final Color TEXT = new Color(15, 23, 42);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color ACCENT = new Color(37, 99, 235);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SHADE = new Color(248, 250, 252);

    private final String trackingBaseUrl;

    public PdfReceiptService(@Value("${swiftdrop.tracking-base-url}") String trackingBaseUrl) {
        this.trackingBaseUrl = trackingBaseUrl.endsWith("/")
                ? trackingBaseUrl.substring(0, trackingBaseUrl.length() - 1)
                : trackingBaseUrl;
    }

    public byte[] generateReceipt(Parcel parcel) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addHeader(document, parcel);
            addDetails(document, parcel);
            addFeeBreakdown(document, parcel);
            addFooter(document, parcel);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not build the receipt PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private void addHeader(Document document, Parcel parcel) {
        Paragraph title = new Paragraph("SwiftDrop", font(FontFactory.HELVETICA_BOLD, 22, TEXT));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Delivery booking receipt", font(FontFactory.HELVETICA, 12, MUTED));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(12);
        document.add(subtitle);

        LineSeparator line = new LineSeparator();
        line.setLineColor(BORDER);
        document.add(new Chunk(line));

        Paragraph tracking = new Paragraph();
        tracking.setSpacingBefore(12);
        tracking.add(new Phrase("Tracking number: ", font(FontFactory.HELVETICA_BOLD, 13, TEXT)));
        tracking.add(new Phrase(parcel.getTrackingNo(), font(FontFactory.COURIER_BOLD, 14, ACCENT)));
        document.add(tracking);

        Paragraph booked = new Paragraph("Booked: " + formatDate(parcel.getCreatedAt()),
                font(FontFactory.HELVETICA, 10, TEXT));
        booked.setSpacingAfter(10);
        document.add(booked);
    }

    private void addDetails(Document document, Parcel parcel) {
        PdfPTable table = twoColumnTable(new float[]{1.3f, 2.7f});
        addRow(table, "Status", humanize(parcel.getStatus()), false);
        addRow(table, "Pickup address", parcel.getPickupAddress(), false);
        addRow(table, "Drop-off address", parcel.getDropoffAddress(), false);
        addRow(table, "Recipient", parcel.getRecipientName(), false);
        addRow(table, "Recipient phone", maskPhone(parcel.getRecipientPhone()), false);
        addRow(table, "Size", humanize(parcel.getSize()), false);
        addRow(table, "Weight", parcel.getWeightKg() != null ? parcel.getWeightKg().toPlainString() + " kg" : null,
                false);
        addRow(table, "Delivery speed", humanize(parcel.getSpeed()), false);
        addRow(table, "Route", humanize(parcel.getRouteType()), false);
        document.add(table);
    }

    /** fee = route base fee + extra weight + size surcharge + express surcharge - promo (architecture §8). */
    private void addFeeBreakdown(Document document, Parcel parcel) {
        Paragraph heading = new Paragraph("Fee breakdown", font(FontFactory.HELVETICA_BOLD, 13, TEXT));
        heading.setSpacingAfter(6);
        document.add(heading);

        String currency = parcel.getFeeCurrency();
        String discountLabel = parcel.getPromoCode() != null
                ? "Promo discount (" + parcel.getPromoCode() + ")"
                : "Promo discount";

        PdfPTable table = twoColumnTable(new float[]{2.8f, 1.2f});
        addRow(table, "Route base fee", money(parcel.getBaseFee(), currency), false);
        addRow(table, "Extra weight", money(parcel.getWeightFee(), currency), false);
        addRow(table, "Size surcharge", money(parcel.getSizeFee(), currency), false);
        addRow(table, "Express surcharge", money(parcel.getExpressFee(), currency), false);
        addRow(table, discountLabel, discount(parcel.getDiscount(), currency), false);
        addRow(table, "Total", money(parcel.getFeeAmount(), currency), true);
        document.add(table);
    }

    private void addFooter(Document document, Parcel parcel) {
        Font footerFont = font(FontFactory.HELVETICA_OBLIQUE, 9, MUTED);
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(18);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.add(new Phrase("Thank you for shipping with SwiftDrop.\n", footerFont));
        footer.add(new Phrase("Track this parcel at " + trackingBaseUrl + "/" + parcel.getTrackingNo(), footerFont));
        document.add(footer);
    }

    /** Hides every digit except the last {@value #VISIBLE_PHONE_DIGITS}, e.g. {@code +94719876543 -> +********543}. */
    static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return NOT_AVAILABLE;
        }
        long digits = phone.chars().filter(Character::isDigit).count();
        long toHide = digits > VISIBLE_PHONE_DIGITS ? digits - VISIBLE_PHONE_DIGITS : digits;
        StringBuilder masked = new StringBuilder(phone.length());
        for (char c : phone.toCharArray()) {
            if (Character.isDigit(c) && toHide > 0) {
                masked.append('*');
                toHide--;
            } else {
                masked.append(c);
            }
        }
        return masked.toString();
    }

    /** {@code LKR 1,250.00}; amounts are stored with two decimals. */
    static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return NOT_AVAILABLE;
        }
        return String.format(Locale.ENGLISH, "%s %,.2f", currency, amount);
    }

    private static String discount(BigDecimal amount, String currency) {
        if (amount == null || amount.signum() == 0) {
            return money(amount, currency);
        }
        return "- " + money(amount, currency);
    }

    /** {@code OUT_FOR_DELIVERY -> Out for delivery}. */
    static String humanize(Enum<?> value) {
        if (value == null) {
            return null;
        }
        String words = value.name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String formatDate(Instant instant) {
        return instant != null ? DATE_FORMAT.format(instant) : NOT_AVAILABLE;
    }

    private static PdfPTable twoColumnTable(float[] widths) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(6);
        table.setSpacingAfter(14);
        return table;
    }

    private static void addRow(PdfPTable table, String label, String value, boolean emphasise) {
        Font labelFont = font(FontFactory.HELVETICA_BOLD, 10, MUTED);
        Font valueFont = font(emphasise ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 10, TEXT);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderColor(BORDER);
        labelCell.setBackgroundColor(SHADE);
        labelCell.setPadding(6);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : NOT_AVAILABLE, valueFont));
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static Font font(String name, float size, Color color) {
        return FontFactory.getFont(name, size, color);
    }
}
