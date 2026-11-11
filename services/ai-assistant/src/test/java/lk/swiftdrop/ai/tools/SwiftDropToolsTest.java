package lk.swiftdrop.ai.tools;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.modelcontextprotocol.common.McpTransportContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lk.swiftdrop.ai.config.GatewayClientConfig;
import lk.swiftdrop.ai.tools.client.GatewayClient;
import lk.swiftdrop.ai.tools.client.GatewayProperties;
import lk.swiftdrop.ai.tools.dto.FeeEstimate;
import lk.swiftdrop.ai.tools.dto.ParcelSummary;
import lk.swiftdrop.ai.tools.dto.TrackingResult;
import lk.swiftdrop.common.event.ParcelStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.web.client.RestClient;

class SwiftDropToolsTest {

    private static final String TOKEN = "user-jwt";

    private static WireMockServer gateway;
    private SwiftDropTools tools;

    @BeforeAll
    static void startGateway() {
        gateway = new WireMockServer(wireMockConfig().dynamicPort());
        gateway.start();
    }

    @AfterAll
    static void stopGateway() {
        gateway.stop();
    }

    @BeforeEach
    void setUp() {
        gateway.resetAll();
        GatewayProperties properties = new GatewayProperties(gateway.baseUrl(), Duration.ofSeconds(2),
                Duration.ofMillis(500));
        RestClient restClient = new GatewayClientConfig().gatewayRestClient(properties);
        tools = new SwiftDropTools(new GatewayClient(restClient));
    }

    @Test
    void trackParcelForwardsTokenAndDropsPersonalData() {
        gateway.stubFor(get("/api/v1/track/SD-7K3Q-9P2M").willReturn(okJson("""
                {"trackingNo":"SD-7K3Q-9P2M","status":"OUT_FOR_DELIVERY","lastUpdatedAt":"2026-11-01T09:15:00Z",
                 "recipientName":"Nimal Perera","recipientPhone":"0771234567","dropoffAddress":"12 Galle Rd",
                 "history":[{"status":"CREATED","at":"2026-10-31T08:00:00Z","note":"call 0771234567"},
                            {"status":"OUT_FOR_DELIVERY","at":"2026-11-01T09:15:00Z"}]}""")));

        TrackingResult result = tools.trackParcel(" sd-7k3q-9p2m ", chatContext(new ToolCallTracker()));

        assertThat(result.status()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(result.history()).extracting(TrackingResult.HistoryEntry::status)
                .containsExactly("CREATED", "OUT_FOR_DELIVERY");
        assertThat(result.toString()).doesNotContain("Nimal", "0771234567", "Galle");
        gateway.verify(getRequestedFor(urlPathEqualTo("/api/v1/track/SD-7K3Q-9P2M"))
                .withHeader("Authorization", equalTo("Bearer " + TOKEN)));
    }

    @Test
    void trackParcelWorksWithoutToken() {
        gateway.stubFor(get("/api/v1/track/SD-AAAA-BBBB")
                .willReturn(okJson("{\"trackingNo\":\"SD-AAAA-BBBB\",\"status\":\"CREATED\",\"history\":[]}")));

        TrackingResult result = tools.trackParcel("SD-AAAA-BBBB", new ToolContext(Map.of()));

        assertThat(result.status()).isEqualTo("CREATED");
        gateway.verify(getRequestedFor(urlPathEqualTo("/api/v1/track/SD-AAAA-BBBB"))
                .withHeader("Authorization", absent()));
    }

    @Test
    void trackParcelRejectsMalformedTrackingNumberWithoutCallingGateway() {
        assertThatThrownBy(() -> tools.trackParcel("12345", chatContext(new ToolCallTracker())))
                .isInstanceOf(ToolFailureException.class)
                .hasMessageContaining("SD-7K3Q-9P2M");
        assertThat(gateway.getAllServeEvents()).isEmpty();
    }

    @Test
    void unknownParcelBecomesShortMessage() {
        gateway.stubFor(get("/api/v1/track/SD-AAAA-BBBB")
                .willReturn(aResponse().withStatus(404).withBody("{\"detail\":\"internal detail\"}")));

        assertThatThrownBy(() -> tools.trackParcel("SD-AAAA-BBBB", chatContext(new ToolCallTracker())))
                .isInstanceOf(ToolFailureException.class)
                .hasMessage("Nothing was found for that request.");
    }

    @Test
    void slowGatewayBecomesUnavailableMessage() {
        gateway.stubFor(get("/api/v1/track/SD-AAAA-BBBB")
                .willReturn(okJson("{}").withFixedDelay(2_000)));

        assertThatThrownBy(() -> tools.trackParcel("SD-AAAA-BBBB", chatContext(new ToolCallTracker())))
                .isInstanceOf(ToolFailureException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void estimateFeeSendsNormalizedQuery() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/pricing/estimate"))
                .withQueryParam("fromZone", equalTo("Colombo South"))
                .withQueryParam("toZone", equalTo("Kandy"))
                .withQueryParam("weightKg", equalTo("2.0"))
                .withQueryParam("size", equalTo("EXTRA_LARGE"))
                .willReturn(okJson("{\"amount\":1250.00,\"currency\":\"LKR\"}")));

        FeeEstimate estimate = tools.estimateFee("Colombo South", "Kandy", new BigDecimal("2.0"), "extra large",
                chatContext(new ToolCallTracker()));

        assertThat(estimate.amount()).isEqualByComparingTo("1250.00");
        assertThat(estimate.currency()).isEqualTo("LKR");
        assertThat(estimate.size()).isEqualTo("EXTRA_LARGE");
    }

    @Test
    void estimateFeeRejectsInvalidArguments() {
        ToolContext context = chatContext(new ToolCallTracker(10));

        assertThatThrownBy(() -> tools.estimateFee("Colombo", "Kandy", new BigDecimal("41"), "SMALL", context))
                .hasMessageContaining("at most 40 kg");
        assertThatThrownBy(() -> tools.estimateFee("Colombo", "Kandy", BigDecimal.ZERO, "SMALL", context))
                .hasMessageContaining("more than 0 kg");
        assertThatThrownBy(() -> tools.estimateFee("Colombo", "Kandy", BigDecimal.ONE, "HUGE", context))
                .hasMessageContaining("SMALL, MEDIUM");
        assertThatThrownBy(() -> tools.estimateFee(" ", "Kandy", BigDecimal.ONE, "SMALL", context))
                .hasMessageContaining("pickup zone");
        assertThat(gateway.getAllServeEvents()).isEmpty();
    }

    @Test
    void estimateFeeRejectsUnexpectedResponse() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/pricing/estimate")).willReturn(okJson("{\"fee\":\"?\"}")));

        assertThatThrownBy(() -> tools.estimateFee("Colombo", "Kandy", BigDecimal.ONE, "SMALL",
                chatContext(new ToolCallTracker())))
                .hasMessage(SwiftDropTools.UNEXPECTED_RESPONSE);
    }

    @Test
    void myParcelsReadsPageAndDropsPersonalData() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/parcels"))
                .withQueryParam("size", equalTo("2"))
                .withQueryParam("status", equalTo("IN_TRANSIT"))
                .willReturn(okJson("""
                        {"content":[
                          {"trackingNo":"SD-AAAA-0001","status":"IN_TRANSIT","createdAt":"2026-10-30T10:00:00Z",
                           "recipientPhone":"0771234567"},
                          {"trackingNo":"SD-AAAA-0002","status":"IN_TRANSIT","createdAt":"2026-10-31T10:00:00Z"},
                          {"trackingNo":"SD-AAAA-0003","status":"IN_TRANSIT","createdAt":"2026-11-01T10:00:00Z"}]}""")));

        List<ParcelSummary> parcels = tools.myParcels(ParcelStatus.IN_TRANSIT, 2, chatContext(new ToolCallTracker()));

        assertThat(parcels).extracting(ParcelSummary::trackingNo).containsExactly("SD-AAAA-0001", "SD-AAAA-0002");
        assertThat(parcels.toString()).doesNotContain("0771234567");
    }

    @Test
    void myParcelsAcceptsPlainArrayAndClampsLimit() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/parcels")).withQueryParam("size", equalTo("10"))
                .willReturn(okJson("[{\"trackingNo\":\"SD-AAAA-0001\",\"status\":\"CREATED\"}]")));

        assertThat(tools.myParcels(null, 50, chatContext(new ToolCallTracker()))).hasSize(1);
    }

    @Test
    void myParcelsNeedsSignedInUser() {
        assertThatThrownBy(() -> tools.myParcels(null, null, new ToolContext(Map.of())))
                .hasMessage(ToolCaller.NOT_SIGNED_IN);
    }

    @Test
    void myParcelsAsCallerWithoutAccessGetsShortMessage() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/parcels")).willReturn(aResponse().withStatus(403)));

        assertThatThrownBy(() -> tools.myParcels(null, null, chatContext(new ToolCallTracker())))
                .hasMessage("The user does not have access to that information.");
    }

    @Test
    void mcpCallUsesAuthorizationHeaderFromTransportContext() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/parcels")).willReturn(okJson("[]")));
        McpTransportContext transport = McpTransportContext.create(
                Map.of(ToolContextKeys.MCP_AUTHORIZATION, "Bearer mcp-jwt"));

        tools.myParcels(null, null, new ToolContext(Map.of(ToolContextKeys.MCP_EXCHANGE, transport)));

        gateway.verify(getRequestedFor(urlPathEqualTo("/api/v1/parcels"))
                .withHeader("Authorization", equalTo("Bearer mcp-jwt")));
    }

    @Test
    void sixthToolCallInOneRequestIsRefused() {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/parcels")).willReturn(okJson("[]")));
        ToolCallTracker tracker = new ToolCallTracker();
        ToolContext context = chatContext(tracker);
        for (int i = 0; i < ToolCallTracker.DEFAULT_MAX_CALLS; i++) {
            tools.myParcels(null, null, context);
        }

        assertThatThrownBy(() -> tools.myParcels(null, null, context))
                .hasMessage(ToolCallTracker.LIMIT_REACHED);
        assertThat(tracker.toolsUsed()).containsExactly(SwiftDropTools.MY_PARCELS);
    }

    @Test
    void toolFailureIsReturnedToTheModelAsText() {
        ToolCallback callback = callback(SwiftDropTools.TRACK_PARCEL);
        ToolExecutionExceptionProcessor processor = DefaultToolExecutionExceptionProcessor.builder().build();

        Throwable thrown = catchThrowable(
                () -> callback.call("{\"trackingNo\":\"bad\"}", chatContext(new ToolCallTracker())));

        assertThat(thrown).isInstanceOf(ToolExecutionException.class);
        assertThat(processor.process((ToolExecutionException) thrown))
                .contains("SD-7K3Q-9P2M").doesNotContain("Exception");
    }

    @Test
    void toolDefinitionsDescribeArgumentsForTheModel() {
        assertThat(callback(SwiftDropTools.ESTIMATE_FEE).getToolDefinition().inputSchema())
                .contains("fromZone", "toZone", "weightKg", "size")
                .doesNotContain("toolContext");
        assertThat(callback(SwiftDropTools.MY_PARCELS).getToolDefinition().inputSchema())
                .contains("OUT_FOR_DELIVERY");
    }

    private ToolCallback callback(String name) {
        for (ToolCallback callback : ToolCallbacks.from(tools)) {
            if (callback.getToolDefinition().name().equals(name)) {
                return callback;
            }
        }
        throw new AssertionError("No tool " + name);
    }

    private static ToolContext chatContext(ToolCallTracker tracker) {
        Map<String, Object> context = new HashMap<>();
        context.put(ToolContextKeys.BEARER_TOKEN, TOKEN);
        context.put(ToolContextKeys.TRACKER, tracker);
        return new ToolContext(context);
    }
}
