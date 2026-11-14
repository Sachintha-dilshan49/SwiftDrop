package lk.swiftdrop.gateway.support;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The real gateway on a random port, with:
 * <ul>
 *   <li>one WireMock server per downstream service; each answers any request with
 *       {@code X-Stub-Service: <name>} so tests can see which service a route reached;</li>
 *   <li>auth's WireMock also serving the JWKS for {@link TestJwts};</li>
 *   <li>Redis from Testcontainers for the rate limiters (flushed before each test).</li>
 * </ul>
 * Small limits keep the tests fast; the response timeout is 5 s. All subclasses share one context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractGatewayIT {

    public static final String STUB_SERVICE_HEADER = "X-Stub-Service";
    public static final int RATE_LIMIT_BURST = 3;

    protected static final TestJwts JWTS = new TestJwts();
    protected static final Map<String, WireMockServer> SERVICES = new LinkedHashMap<>();

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        for (String name : new String[] {"auth", "pricing", "parcel", "dispatch", "merchant", "notification",
                "assistant"}) {
            WireMockServer server = new WireMockServer(options().dynamicPort());
            server.start();
            SERVICES.put(name, server);
        }
        REDIS.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    private ReactiveStringRedisTemplate redis;

    protected WebTestClient client;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> service("auth").baseUrl());
        registry.add("PRICING_SERVICE_URL", () -> service("pricing").baseUrl());
        registry.add("PARCEL_SERVICE_URL", () -> service("parcel").baseUrl());
        registry.add("DISPATCH_SERVICE_URL", () -> service("dispatch").baseUrl());
        registry.add("MERCHANT_SERVICE_URL", () -> service("merchant").baseUrl());
        registry.add("NOTIFICATION_SERVICE_URL", () -> service("notification").baseUrl());
        registry.add("AI_ASSISTANT_URL", () -> service("assistant").baseUrl());
        registry.add("JWT_JWK_SET_URI", () -> service("auth").baseUrl() + "/.well-known/jwks.json");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // Long enough for the first, cold request through a new context; ResilienceIT's slow stub is slower
        registry.add("GATEWAY_HTTP_RESPONSE_TIMEOUT", () -> "5s");
        registry.add("GATEWAY_MERCHANT_RATE_LIMIT_REPLENISH", () -> 1);
        registry.add("GATEWAY_MERCHANT_RATE_LIMIT_BURST", () -> RATE_LIMIT_BURST);
        registry.add("GATEWAY_ESTIMATE_RATE_LIMIT_REPLENISH", () -> 1);
        registry.add("GATEWAY_ESTIMATE_RATE_LIMIT_BURST", () -> RATE_LIMIT_BURST);
    }

    @BeforeEach
    void resetStubsAndLimits() {
        SERVICES.forEach((name, server) -> {
            server.resetAll();
            server.stubFor(any(anyUrl()).atPriority(10)
                    .willReturn(okJson("{\"service\":\"" + name + "\"}").withHeader(STUB_SERVICE_HEADER, name)));
        });
        service("auth").stubFor(get(urlEqualTo("/.well-known/jwks.json")).atPriority(1)
                .willReturn(okJson(JWTS.jwksJson())));
        redis.execute(connection -> connection.serverCommands().flushAll()).blockLast(Duration.ofSeconds(5));
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    protected static WireMockServer service(String name) {
        return SERVICES.get(name);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Requests every stub received, except auth's JWKS fetches. */
    protected static int forwardedRequestCount() {
        return SERVICES.values().stream()
                .mapToInt(server -> (int) server.getAllServeEvents().stream()
                        .filter(event -> !event.getRequest().getUrl().startsWith("/.well-known/"))
                        .count())
                .sum();
    }
}
