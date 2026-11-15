package lk.swiftdrop.ai.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lk.swiftdrop.ai.tools.client.GatewayClient;
import lk.swiftdrop.ai.tools.dto.FeeEstimate;
import lk.swiftdrop.ai.tools.dto.ParcelSummary;
import lk.swiftdrop.ai.tools.dto.TrackingResult;
import lk.swiftdrop.common.event.ParcelStatus;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Read-only tools over SwiftDrop's public API, used by the chat and exposed over MCP.
 *
 * <p>Each tool validates its arguments (the model can send anything), calls the gateway as the
 * caller and copies only the fields the model needs, so personal data in the service responses
 * (recipient name, phone, address, courier notes) never reaches the LLM.
 */
@Component
public class SwiftDropTools {

    public static final String TRACK_PARCEL = "track_parcel";
    public static final String ESTIMATE_FEE = "estimate_fee";
    public static final String MY_PARCELS = "my_parcels";

    static final Pattern TRACKING_NO = Pattern.compile("^SD-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$");
    static final int MAX_ZONE_LENGTH = 64;
    static final List<String> SIZES = List.of("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE");
    static final List<String> SPEEDS = List.of("EXPRESS", "STANDARD", "NEXT_DAY");
    static final String DEFAULT_SPEED = "STANDARD";
    static final BigDecimal MAX_WEIGHT_KG = BigDecimal.valueOf(40);
    static final int MAX_PARCELS = 10;
    static final String UNEXPECTED_RESPONSE = "SwiftDrop returned an unexpected answer. Try again later.";

    private final GatewayClient gateway;

    public SwiftDropTools(GatewayClient gateway) {
        this.gateway = gateway;
    }

    @Tool(name = TRACK_PARCEL, description = """
            Get the current status and status history of a SwiftDrop parcel by its tracking number. \
            Use it when the user asks where a parcel is or what its status means.""")
    public TrackingResult trackParcel(
            @ToolParam(description = "Tracking number in the format SD-XXXX-XXXX, e.g. SD-7K3Q-9P2M") String trackingNo,
            ToolContext toolContext) {
        String token = ToolCaller.begin(toolContext, TRACK_PARCEL);
        String normalized = trackingNo == null ? "" : trackingNo.trim().toUpperCase(Locale.ROOT);
        if (!TRACKING_NO.matcher(normalized).matches()) {
            throw new ToolFailureException("A tracking number looks like SD-7K3Q-9P2M. Ask the user to check it.");
        }
        JsonNode body = gateway.get(uri -> uri.path("/api/v1/track/{trackingNo}").build(normalized), token);

        List<TrackingResult.HistoryEntry> history = new ArrayList<>();
        String lastUpdatedAt = text(body, "createdAt");
        for (JsonNode entry : body.path("history").values()) {
            history.add(new TrackingResult.HistoryEntry(FriendlyText.statusLabel(text(entry, "status")),
                    FriendlyText.time(text(entry, "occurredAt"))));
            // parcel-service returns history oldest first and has no separate last-updated field
            lastUpdatedAt = text(entry, "occurredAt");
        }
        String status = text(body, "status");
        return new TrackingResult(normalized, FriendlyText.statusLabel(status), FriendlyText.statusMeaning(status),
                FriendlyText.time(lastUpdatedAt), history);
    }

    @Tool(name = ESTIMATE_FEE, description = """
            Estimate the delivery fee in LKR for a parcel between two SwiftDrop zones. \
            Ask the user for a missing zone, weight or size instead of guessing. \
            The result shows the delivery speed used. The booking step gives the final fee.""")
    public FeeEstimate estimateFee(
            @ToolParam(description = "Pickup zone or area, e.g. Colombo South or Nugegoda") String pickupZone,
            @ToolParam(description = "Drop-off zone or area, e.g. Kandy") String dropoffZone,
            @ToolParam(description = "Parcel weight in kilograms, greater than 0 and at most 40") BigDecimal weightKg,
            @ToolParam(description = "Parcel size: SMALL, MEDIUM, LARGE or EXTRA_LARGE") String size,
            @ToolParam(required = false, description = "Delivery speed: EXPRESS, STANDARD or NEXT_DAY (default STANDARD)")
            String speed,
            ToolContext toolContext) {
        String token = ToolCaller.begin(toolContext, ESTIMATE_FEE);
        String pickup = zoneInput(pickupZone, "pickup");
        String dropoff = zoneInput(dropoffZone, "drop-off");
        // pricing-service accepts at most 2 decimals
        BigDecimal weight = weightKg == null || weightKg.scale() <= 2 ? weightKg : weightKg.setScale(2, RoundingMode.HALF_UP);
        if (weight == null || weight.signum() <= 0 || weight.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new ToolFailureException("The weight must be more than 0 kg and at most 40 kg.");
        }
        String normalizedSize = enumInput(size);
        if (!SIZES.contains(normalizedSize)) {
            throw new ToolFailureException("The size must be SMALL, MEDIUM, LARGE or EXTRA_LARGE.");
        }
        String normalizedSpeed = speed == null || speed.isBlank() ? DEFAULT_SPEED : enumInput(speed);
        if (!SPEEDS.contains(normalizedSpeed)) {
            throw new ToolFailureException("The delivery speed must be EXPRESS, STANDARD or NEXT_DAY.");
        }

        JsonNode zones = gateway.get(uri -> uri.path("/api/v1/pricing/zones").build(), token);
        Zone from = findZone(zones, pickup);
        Zone to = findZone(zones, dropoff);
        JsonNode body = gateway.get(uri -> uri.path("/api/v1/pricing/estimate")
                .queryParam("pickupZone", from.code())
                .queryParam("dropoffZone", to.code())
                .queryParam("weightKg", weight.toPlainString())
                .queryParam("size", normalizedSize)
                .queryParam("speed", normalizedSpeed)
                .build(), token);
        JsonNode total = body.path("total");
        if (!total.isNumber()) {
            throw new ToolFailureException(UNEXPECTED_RESPONSE);
        }
        String currency = body.path("currency").asString("LKR");
        return new FeeEstimate(from.name(), to.name(), weight, FriendlyText.words(normalizedSize),
                FriendlyText.words(normalizedSpeed), total.asDecimal(),
                currency);
    }

    @Tool(name = MY_PARCELS, description = """
            List the signed-in user's own most recent parcels with tracking number, status and creation date. \
            Only customers and merchants have parcels.""")
    public List<ParcelSummary> myParcels(
            @ToolParam(required = false, description = "Only parcels with this status") ParcelStatus status,
            @ToolParam(required = false, description = "How many parcels to return, 1 to 10 (default 5)") Integer limit,
            ToolContext toolContext) {
        String token = ToolCaller.beginSignedIn(toolContext, MY_PARCELS);
        int size = limit == null ? 5 : Math.clamp(limit, 1, MAX_PARCELS);
        JsonNode body = gateway.get(uri -> {
            uri.path("/api/v1/parcels").queryParam("size", size);
            if (status != null) {
                uri.queryParam("status", status.name());
            }
            return uri.build();
        }, token);

        JsonNode items = body.isArray() ? body : body.path("content");
        List<ParcelSummary> parcels = new ArrayList<>();
        for (JsonNode item : items.values()) {
            if (parcels.size() == size) {
                break;
            }
            parcels.add(new ParcelSummary(text(item, "trackingNo"), FriendlyText.statusLabel(text(item, "status")),
                    FriendlyText.time(text(item, "createdAt"))));
        }
        return parcels;
    }

    private static String zoneInput(String value, String which) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_ZONE_LENGTH) {
            throw new ToolFailureException("Ask the user for the " + which + " zone, e.g. Colombo South.");
        }
        return trimmed;
    }

    private static String enumInput(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    /** Matches what the user said against zone codes and names first, then the districts inside each zone. */
    private static Zone findZone(JsonNode zones, String input) {
        String wanted = comparable(input);
        List<String> names = new ArrayList<>();
        for (JsonNode zone : zones.values()) {
            names.add(zone.path("name").asString(""));
            if (wanted.equals(comparable(zone.path("code").asString("")))
                    || wanted.equals(comparable(zone.path("name").asString("")))) {
                return new Zone(zone.path("code").asString(), zone.path("name").asString());
            }
        }
        for (JsonNode zone : zones.values()) {
            for (JsonNode district : zone.path("districts").values()) {
                if (wanted.equals(comparable(district.asString("")))) {
                    return new Zone(zone.path("code").asString(), zone.path("name").asString());
                }
            }
        }
        throw new ToolFailureException("SwiftDrop has no zone called " + input + ". The zones are: "
                + String.join(", ", names) + ".");
    }

    /** Ignores case, punctuation and leading zeros, so "colombo 4" matches "Colombo 04". */
    private static String comparable(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\b0+(\\d)", "$1");
    }

    private record Zone(String code, String name) {
    }

    /** First non-empty text value among {@code names}, or {@code null}. */
    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isValueNode() && !value.isNull()) {
                return value.asString();
            }
        }
        return null;
    }
}
