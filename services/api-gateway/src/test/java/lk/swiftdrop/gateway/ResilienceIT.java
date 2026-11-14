package lk.swiftdrop.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.Fault;
import java.time.Duration;
import java.util.List;
import lk.swiftdrop.gateway.support.AbstractGatewayIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.util.function.Tuple2;

/** SSE streaming, timeouts and downstream failures. The general response timeout is 5 s in tests. */
class ResilienceIT extends AbstractGatewayIT {

    @Test
    void sseEventsArriveAsTheyAreSentNotAtTheEnd() {
        // Two equal-sized events, sent as two chunks about 6 s apart: longer than the 5 s general timeout,
        // so the stream only survives because the SSE route has none
        String body = "data: one\n\n" + "data: two\n\n";
        service("parcel").stubFor(get(urlEqualTo("/api/v1/track/SD-1/stream")).willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .withBody(body)
                .withChunkedDribbleDelay(2, 12000)));

        List<Tuple2<Long, String>> events = client.get().uri("/api/v1/track/SD-1/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(String.class)
                .getResponseBody()
                .elapsed()
                .collectList()
                .block(Duration.ofSeconds(30));

        assertThat(events).extracting(Tuple2::getT2).containsExactly("one", "two");
        // If the gateway buffered, both would arrive together; the gap is longer than the general timeout
        assertThat(events.get(1).getT1()).isGreaterThan(5000);
    }

    @Test
    void slowDownstreamIsGatewayTimeoutProblem() {
        service("dispatch").stubFor(get(urlEqualTo("/api/v1/dispatch/unassigned"))
                .willReturn(okJson("{}").withFixedDelay(8000)));

        String body = client.get().uri("/api/v1/dispatch/unassigned")
                .header(HttpHeaders.AUTHORIZATION, bearer(JWTS.valid()))
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists("X-Request-Id")
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(body)
                .contains("\"type\":\"https://swiftdrop.lk/problems/gateway-timeout\"")
                .contains("\"detail\":\"The service did not respond in time.\"")
                .contains("\"instance\":\"/api/v1/dispatch/unassigned\"")
                .doesNotContain("localhost").doesNotContain("127.0.0.1").doesNotContain("5000ms")
                .doesNotContain(String.valueOf(service("dispatch").port()));
    }

    @Test
    void connectionDroppedByDownstreamIsServiceUnavailableProblem() {
        service("parcel").stubFor(get(urlEqualTo("/api/v1/track/SD-1"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        client.get().uri("/api/v1/track/SD-1")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://swiftdrop.lk/problems/service-unavailable")
                .jsonPath("$.detail").isEqualTo("Service temporarily unavailable");
    }

    @Test
    void downstreamProblemDetailPassesThroughUntouched() {
        String problem = "{\"type\":\"https://swiftdrop.lk/problems/invalid-status-transition\","
                + "\"title\":\"Invalid status transition\",\"status\":409,"
                + "\"detail\":\"Parcel cannot move from CREATED to DELIVERED.\","
                + "\"instance\":\"/api/v1/parcels/c41d\",\"requestId\":\"from-service\"}";
        service("parcel").stubFor(get(urlEqualTo("/api/v1/parcels/c41d")).willReturn(aResponse()
                .withStatus(409)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .withBody(problem)));

        client.get().uri("/api/v1/parcels/c41d")
                .header(HttpHeaders.AUTHORIZATION, bearer(JWTS.valid()))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(String.class).isEqualTo(problem);
    }
}
