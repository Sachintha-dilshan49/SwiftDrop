package lk.swiftdrop.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lk.swiftdrop.gateway.support.AbstractGatewayIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.FluxExchangeResult;

class CorsIT extends AbstractGatewayIT {

    private static final String SPA = "http://localhost:3000";

    @Test
    void preflightFromSpaOriginSucceedsWithoutToken() {
        client.options().uri("/api/v1/parcels")
                .header(HttpHeaders.ORIGIN, SPA)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, X-Request-Id")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SPA)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

        // Preflight is answered by the gateway, never forwarded
        assertThat(forwardedRequestCount()).isZero();
    }

    @Test
    void preflightForMerchantApiKeyHeaderSucceeds() {
        client.options().uri("/api/v1/merchant-api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-API-Key")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void disallowedOriginGetsNoCorsHeaders() {
        FluxExchangeResult<String> result = client.options().uri("/api/v1/parcels")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .returnResult(String.class);

        assertThat(result.getStatus().is2xxSuccessful()).isFalse();
        assertThat(result.getResponseHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
        assertThat(forwardedRequestCount()).isZero();
    }

    @Test
    void actualRequestExposesRequestIdAndDedupesCorsHeadersAServiceAlsoSets() {
        service("parcel").stubFor(get(urlEqualTo("/api/v1/track/SD-1")).willReturn(okJson("{}")
                .withHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SPA)
                .withHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")));

        HttpHeaders headers = client.get().uri("/api/v1/track/SD-1")
                .header(HttpHeaders.ORIGIN, SPA)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class).getResponseHeaders();

        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).containsExactly(SPA);
        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).containsExactly("true");
        assertThat(String.join(",", headers.getOrEmpty(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)))
                .contains("X-Request-Id");
    }

    @Test
    void unauthorizedResponseStillHasCorsHeadersSoTheSpaCanReadIt() {
        List<String> origin = client.get().uri("/api/v1/parcels")
                .header(HttpHeaders.ORIGIN, SPA)
                .exchange()
                .expectStatus().isUnauthorized()
                .returnResult(String.class).getResponseHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);

        assertThat(origin).containsExactly(SPA);
    }
}
