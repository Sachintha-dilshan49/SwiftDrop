package lk.swiftdrop.ai.tools;

import java.math.BigDecimal;
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

    static final Pattern TRACKING_NO = Pattern.compile("^SD-[A-Z0-9]{4}-[A-Z0-9]{4}$");
    static final Pattern ZONE = Pattern.compile("^[A-Za-z0-9 _-]{1,64}$");
    static final List<String> SIZES = List.of("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE");
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
        for (JsonNode entry : body.path("history").values()) {
            history.add(new TrackingResult.HistoryEntry(text(entry, "status"), text(entry, "at", "changedAt")));
        }
        return new TrackingResult(normalized, text(body, "status"), text(body, "lastUpdatedAt", "updatedAt"),
                history);
    }

    @Tool(name = ESTIMATE_FEE, description = """
            Estimate the delivery fee in LKR for a parcel between two SwiftDrop zones. \
            Ask the user for any missing value instead of guessing. The booking step gives the final fee.""")
    public FeeEstimate estimateFee(
            @ToolParam(description = "Pickup zone, e.g. Colombo South") String fromZone,
            @ToolParam(description = "Drop-off zone, e.g. Kandy") String toZone,
            @ToolParam(description = "Parcel weight in kilograms, greater than 0 and at most 40") BigDecimal weightKg,
            @ToolParam(description = "Parcel size: SMALL, MEDIUM, LARGE or EXTRA_LARGE") String size,
            ToolContext toolContext) {
        String token = ToolCaller.begin(toolContext, ESTIMATE_FEE);
        String from = zone(fromZone, "pickup");
        String to = zone(toZone, "drop-off");
        if (weightKg == null || weightKg.signum() <= 0 || weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new ToolFailureException("The weight must be more than 0 kg and at most 40 kg.");
        }
        String normalizedSize = size == null ? "" : size.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!SIZES.contains(normalizedSize)) {
            throw new ToolFailureException("The size must be SMALL, MEDIUM, LARGE or EXTRA_LARGE.");
        }

        JsonNode body = gateway.get(uri -> uri.path("/api/v1/pricing/estimate")
                .queryParam("fromZone", from)
                .queryParam("toZone", to)
                .queryParam("weightKg", weightKg.toPlainString())
                .queryParam("size", normalizedSize)
                .build(), token);
        JsonNode amount = body.path("amount").isMissingNode() ? body.path("total") : body.path("amount");
        if (!amount.isNumber() && !amount.isString()) {
            throw new ToolFailureException(UNEXPECTED_RESPONSE);
        }
        String currency = body.path("currency").asString("LKR");
        return new FeeEstimate(from, to, weightKg, normalizedSize, amount.asDecimal(), currency);
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
            parcels.add(new ParcelSummary(text(item, "trackingNo"), text(item, "status"), text(item, "createdAt")));
        }
        return parcels;
    }

    private static String zone(String value, String which) {
        String trimmed = value == null ? "" : value.trim();
        if (!ZONE.matcher(trimmed).matches()) {
            throw new ToolFailureException("Ask the user for the " + which + " zone, e.g. Colombo South.");
        }
        return trimmed;
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
