package lk.swiftdrop.ai.mcp;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import lk.swiftdrop.ai.knowledge.KnowledgeTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = KnowledgeTestConfig.PROPERTY)
@Import(KnowledgeTestConfig.class)
@AutoConfigureMockMvc
class McpServerIT {

    private static final WireMockServer gateway = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        gateway.start();
    }

    @DynamicPropertySource
    static void gatewayUrl(DynamicPropertyRegistry registry) {
        registry.add("swiftdrop.gateway.base-url", gateway::baseUrl);
    }

    @AfterAll
    static void stopGateway() {
        gateway.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        gateway.resetAll();
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> Jwt.withTokenValue(invocation.getArgument(0))
                .header("alg", "RS256")
                .subject("8b1f6a2e-1111-4c2e-9a71-0d6f2b9c1e44")
                .claim("roles", List.of("CUSTOMER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
    }

    @Test
    void mcpRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(initialize()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void initializeReturnsServerInfo() throws Exception {
        rpc(initialize())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serverInfo.name").value("swiftdrop"))
                .andExpect(jsonPath("$.result.capabilities.tools").exists());
    }

    @Test
    void toolsListContainsSwiftDropTools() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[*].name",
                        Matchers.containsInAnyOrder("track_parcel", "estimate_fee", "my_parcels",
                                "search_help_articles")));
    }

    @Test
    void toolCallForwardsCallersToken() throws Exception {
        gateway.stubFor(get(urlPathEqualTo("/api/v1/parcels")).willReturn(okJson("""
                [{"trackingNo":"SD-AAAA-0001","status":"IN_TRANSIT","createdAt":"2026-11-01T10:00:00Z"}]""")));

        rpc("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"my_parcels","arguments":{}}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                .andExpect(jsonPath("$.result.content[0].text", Matchers.containsString("SD-AAAA-0001")));

        gateway.verify(getRequestedFor(urlPathEqualTo("/api/v1/parcels"))
                .withHeader("Authorization", equalTo("Bearer mcp-user-token")));
    }

    @Test
    void toolFailureIsReturnedAsToolError() throws Exception {
        rpc("""
                {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"track_parcel",
                 "arguments":{"trackingNo":"not-a-number"}}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(true))
                .andExpect(jsonPath("$.result.content[0].text", Matchers.containsString("SD-7K3Q-9P2M")));
    }

    private ResultActions rpc(String body) throws Exception {
        return mockMvc.perform(post("/mcp")
                .header("Authorization", "Bearer mcp-user-token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .content(body));
    }

    private static String initialize() {
        return """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
                 "capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}}}""";
    }
}
