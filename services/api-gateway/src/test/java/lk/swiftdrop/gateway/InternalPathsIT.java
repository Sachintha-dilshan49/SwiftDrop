package lk.swiftdrop.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import lk.swiftdrop.gateway.support.AbstractGatewayIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class InternalPathsIT extends AbstractGatewayIT {

    @ParameterizedTest
    @ValueSource(strings = {"/internal/users/1/contact", "/internal", "/api/v1/internal/parcels/1/status",
            "/api/v1/internal"})
    void internalPathsLookMissingEvenWithAToken(String path) {
        client.patch().uri(path)
                .header(HttpHeaders.AUTHORIZATION, bearer(JWTS.valid()))
                .header("X-Internal-Token", "guessed")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/not-found")
                .jsonPath("$.instance").isEqualTo(path);

        assertThat(forwardedRequestCount()).isZero();
    }

    @Test
    void internalPathsWithoutTokenAreNotFoundNotUnauthorized() {
        client.get().uri("/internal/merchants/1/webhook")
                .exchange()
                .expectStatus().isNotFound();
        assertThat(forwardedRequestCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/parcels/../../../internal/parcels/1/contacts", "/%69nternal/users/1/contact",
            "/api/v1/track/x/..%2F..%2F..%2F..%2Finternal/users", "/internal;x=1/users/1/contact",
            "//internal/users/1/contact"})
    void disguisedInternalPathsAreNeverForwarded(String rawPath) {
        HttpStatus status = HttpStatus.resolve(client.get().uri(URI.create("http://localhost:" + port + rawPath))
                .header(HttpHeaders.AUTHORIZATION, bearer(JWTS.valid()))
                .exchange()
                .returnResult(String.class)
                .getStatus().value());

        assertThat(status).isIn(HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);
        assertThat(forwardedRequestCount()).isZero();
    }

    @Test
    void incomingInternalTokenIsStrippedBeforeForwarding() {
        client.get().uri("/api/v1/track/SD-1")
                .header("X-Internal-Token", "from-the-internet")
                .exchange()
                .expectStatus().isOk();

        service("parcel").verify(getRequestedFor(urlEqualTo("/api/v1/track/SD-1")).withoutHeader("X-Internal-Token"));
    }

    @Test
    void otherHeadersStillReachTheService() {
        client.get().uri("/api/v1/track/SD-1")
                .header("X-Custom", "kept")
                .exchange()
                .expectStatus().isOk();

        service("parcel").verify(getRequestedFor(urlEqualTo("/api/v1/track/SD-1")).withHeader("X-Custom",
                equalTo("kept")));
    }
}
