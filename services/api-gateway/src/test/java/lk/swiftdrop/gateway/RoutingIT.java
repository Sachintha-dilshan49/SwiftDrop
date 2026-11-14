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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RoutingIT extends AbstractGatewayIT {

    static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of("/api/v1/auth/me", "auth"),
                Arguments.of("/api/v1/oauth2/authorization/google", "auth"),
                Arguments.of("/oauth2/authorization/google", "auth"),
                Arguments.of("/login/oauth2/code/google?code=abc&state=xyz", "auth"),
                Arguments.of("/api/v1/admin/users/42", "auth"),
                Arguments.of("/api/v1/pricing/zones", "pricing"),
                Arguments.of("/api/v1/pricing/estimate?pickupZone=A&dropoffZone=B&weightKg=1", "pricing"),
                Arguments.of("/api/v1/admin/zones/7", "pricing"),
                Arguments.of("/api/v1/admin/pricing-rules", "pricing"),
                Arguments.of("/api/v1/admin/promo-codes", "pricing"),
                Arguments.of("/api/v1/admin/promo-codes/9", "pricing"),
                Arguments.of("/api/v1/parcels/c41d", "parcel"),
                Arguments.of("/api/v1/track/SD-7K3Q-9P2M", "parcel"),
                Arguments.of("/api/v1/dispatch/unassigned", "dispatch"),
                Arguments.of("/api/v1/courier/deliveries/today", "dispatch"),
                Arguments.of("/api/v1/reports/daily?from=2026-11-01&to=2026-11-05", "dispatch"),
                Arguments.of("/api/v1/merchants/me/orders", "merchant"),
                Arguments.of("/api/v1/admin/merchants?status=PENDING", "merchant"),
                Arguments.of("/api/v1/admin/webhook-deliveries", "notification"),
                Arguments.of("/api/v1/assistant/chat", "assistant"),
                Arguments.of("/api/v1/assistant/models", "assistant"),
                Arguments.of("/mcp", "assistant"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("routes")
    void routeReachesOwningServiceWithFullPathAndAuthorizationIntact(String pathAndQuery, String expectedService) {
        String authorization = bearer(JWTS.valid());

        client.get().uri(pathAndQuery)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(STUB_SERVICE_HEADER, expectedService);

        service(expectedService).verify(1, getRequestedFor(urlEqualTo(pathAndQuery))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(authorization)));
        SERVICES.forEach((name, server) -> {
            if (!name.equals(expectedService)) {
                server.verify(0, anyRequestedFor(anyUrl()).withUrl(pathAndQuery));
            }
        });
    }

    @Test
    void adminPathsGoToTheirOwnersNotToABroaderMatch() {
        String token = bearer(JWTS.valid());
        assertOwner("/api/v1/admin/users", "auth", token);
        assertOwner("/api/v1/admin/zones", "pricing", token);
        assertOwner("/api/v1/admin/promo-codes", "pricing", token);
        assertOwner("/api/v1/admin/merchants/9/approve", "merchant", token);
        assertOwner("/api/v1/admin/webhook-deliveries/5/replay", "notification", token);
    }

    @Test
    void unknownPathWithValidTokenIsNotFoundProblem() {
        client.get().uri("/api/v1/nothing-here")
                .header(HttpHeaders.AUTHORIZATION, bearer(JWTS.valid()))
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/not-found")
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.instance").isEqualTo("/api/v1/nothing-here")
                .jsonPath("$.requestId").isNotEmpty();
        assertThat(forwardedRequestCount()).isZero();
    }

    private void assertOwner(String path, String owner, String authorization) {
        client.post().uri(path)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(STUB_SERVICE_HEADER, owner);
    }
}
