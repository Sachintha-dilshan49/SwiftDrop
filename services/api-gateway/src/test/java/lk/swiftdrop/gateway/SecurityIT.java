package lk.swiftdrop.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import lk.swiftdrop.gateway.support.AbstractGatewayIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class SecurityIT extends AbstractGatewayIT {

    @Test
    void protectedPathWithoutTokenIsUnauthorizedProblemAndNotForwarded() {
        client.get().uri("/api/v1/parcels")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().exists("X-Request-Id")
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/unauthorized")
                .jsonPath("$.title").isEqualTo("Unauthorized")
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.detail").isEqualTo("Authentication is required to access this resource.")
                .jsonPath("$.instance").isEqualTo("/api/v1/parcels")
                .jsonPath("$.requestId").isNotEmpty();

        assertThat(forwardedRequestCount()).isZero();
    }

    static Stream<Arguments> badTokens() {
        return Stream.of(
                Arguments.of("expired", JWTS.expired()),
                Arguments.of("wrong signature", JWTS.wrongSignature()),
                Arguments.of("not a JWT", "abc.def.ghi"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("badTokens")
    void invalidTokenIsUnauthorizedWithoutSayingWhy(String description, String token) {
        byte[] body = client.get().uri("/api/v1/parcels")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/unauthorized")
                .jsonPath("$.detail").isEqualTo("Authentication is required to access this resource.")
                .returnResult().getResponseBody();

        assertThat(new String(body)).doesNotContainIgnoringCase("expired").doesNotContainIgnoringCase("signature");
        assertThat(forwardedRequestCount()).isZero();
    }

    @Test
    void validTokenIsForwardedWithAuthorizationUnchanged() {
        String authorization = bearer(JWTS.valid());

        client.get().uri("/api/v1/parcels")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(STUB_SERVICE_HEADER, "parcel");

        service("parcel").verify(getRequestedFor(urlEqualTo("/api/v1/parcels"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(authorization)));
    }

    static Stream<Arguments> publicPaths() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/auth/login", "auth"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/signup", "auth"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/register", "auth"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/refresh", "auth"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/logout", "auth"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/token", "auth"),
                Arguments.of(HttpMethod.GET, "/api/v1/oauth2/authorization/google", "auth"),
                Arguments.of(HttpMethod.GET, "/oauth2/authorization/google", "auth"),
                Arguments.of(HttpMethod.GET, "/login/oauth2/code/google?code=c&state=s", "auth"),
                Arguments.of(HttpMethod.GET, "/api/v1/track/SD-7K3Q-9P2M", "parcel"),
                Arguments.of(HttpMethod.GET, "/api/v1/track/SD-7K3Q-9P2M/stream", "parcel"),
                Arguments.of(HttpMethod.GET, "/api/v1/pricing/estimate?pickupZone=A&dropoffZone=B", "pricing"),
                Arguments.of(HttpMethod.GET, "/api/v1/pricing/zones", "pricing"),
                Arguments.of(HttpMethod.POST, "/api/v1/merchants/register", "merchant"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("publicPaths")
    void publicPathWorksWithoutToken(HttpMethod method, String path, String expectedService) {
        client.method(method).uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(STUB_SERVICE_HEADER, expectedService);
    }

    @Test
    void publicPathIgnoresAStaleTokenAndStillForwardsIt() {
        String stale = bearer(JWTS.expired());

        client.post().uri("/api/v1/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, stale)
                .exchange()
                .expectStatus().isOk();

        service("auth").verify(anyRequestedFor(urlEqualTo("/api/v1/auth/refresh"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(stale)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET /api/v1/auth/me", "POST /api/v1/pricing/estimate", "GET /api/v1/merchants/register",
            "POST /api/v1/pricing/zones"})
    void publicRuleIsMethodSpecific(String request) {
        String[] parts = request.split(" ");
        client.method(HttpMethod.valueOf(parts[0])).uri(parts[1])
                .exchange()
                .expectStatus().isUnauthorized();
        SERVICES.values().forEach(server -> server.verify(0, anyRequestedFor(urlEqualTo(parts[1]))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/admin/zones", "/api/v1/admin/pricing-rules", "/api/v1/admin/promo-codes"})
    void pricingAdminPathsNeedAToken(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().isUnauthorized();
        SERVICES.values().forEach(server -> server.verify(0, anyRequestedFor(urlEqualTo(path))));
    }

    @Test
    void healthIsPublicAndOtherActuatorPathsAreNotFound() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
        client.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
        client.get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();

        WebTestClient authed = client.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer(JWTS.valid()))
                .build();
        for (String path : new String[] {"/actuator", "/actuator/env", "/actuator/info", "/actuator/gateway/routes",
                "/actuator/beans"}) {
            authed.get().uri(path).exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        }
        assertThat(forwardedRequestCount()).isZero();
        SERVICES.values().forEach(server -> server.verify(0, anyRequestedFor(anyUrl()).withUrl("/actuator/env")));
    }
}
