package lk.swiftdrop.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.gateway.support.AbstractGatewayIT;
import org.junit.jupiter.api.Test;

class RequestIdIT extends AbstractGatewayIT {

    private static final String HEADER = "X-Request-Id";

    @Test
    void generatesIdWhenAbsentAndForwardsIt() {
        String id = client.get().uri("/api/v1/track/SD-1")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class).getResponseHeaders().getFirst(HEADER);

        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(singleForwardedHeader()).isEqualTo(id);
    }

    @Test
    void keepsValidIncomingId() {
        client.get().uri("/api/v1/track/SD-1")
                .header(HEADER, "client-id_42.a")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HEADER, "client-id_42.a");

        service("parcel").verify(getRequestedFor(urlEqualTo("/api/v1/track/SD-1"))
                .withHeader(HEADER, equalTo("client-id_42.a")));
    }

    @Test
    void replacesInvalidIncomingIdOnRequestAndResponse() {
        String id = client.get().uri("/api/v1/track/SD-1")
                .header(HEADER, "bad id <script>")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class).getResponseHeaders().getFirst(HEADER);

        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(singleForwardedHeader()).isEqualTo(id);
    }

    @Test
    void responseKeepsOnlyTheGatewayIdWhenServiceEchoesOne() {
        service("parcel").stubFor(get(urlEqualTo("/api/v1/track/SD-1"))
                .willReturn(okJson("{}").withHeader(HEADER, "set-by-service")));

        List<String> ids = client.get().uri("/api/v1/track/SD-1")
                .header(HEADER, "abc-123")
                .exchange()
                .returnResult(String.class).getResponseHeaders().get(HEADER);

        assertThat(ids).containsExactly("abc-123");
    }

    @Test
    void unauthorizedResponseCarriesTheId() {
        client.get().uri("/api/v1/parcels")
                .header(HEADER, "trace-401")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HEADER, "trace-401")
                .expectBody().jsonPath("$.requestId").isEqualTo("trace-401");
    }

    @Test
    void notFoundResponseCarriesTheId() {
        client.get().uri("/internal/users/1/contact")
                .header(HEADER, "trace-404")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().valueEquals(HEADER, "trace-404")
                .expectBody().jsonPath("$.requestId").isEqualTo("trace-404");
    }

    private static String singleForwardedHeader() {
        List<LoggedRequest> requests = service("parcel").findAll(getRequestedFor(urlEqualTo("/api/v1/track/SD-1")));
        assertThat(requests).hasSize(1);
        List<String> values = requests.get(0).getHeaders().getHeader(HEADER).values();
        assertThat(values).hasSize(1);
        return values.get(0);
    }
}
