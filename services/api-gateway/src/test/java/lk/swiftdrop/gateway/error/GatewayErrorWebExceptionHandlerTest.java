package lk.swiftdrop.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import lk.swiftdrop.gateway.web.RequestIdWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class GatewayErrorWebExceptionHandlerTest {

    private final GatewayErrorWebExceptionHandler handler =
            new GatewayErrorWebExceptionHandler(new ProblemResponseWriter(JsonMapper.builder().build()));

    @Test
    void mapsExceptionsToStatuses() {
        assertThat(GatewayErrorWebExceptionHandler.statusFor(new ConnectException("Connection refused: host:8083")))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(GatewayErrorWebExceptionHandler.statusFor(
                new RuntimeException(new UnknownHostException("parcel-service"))))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(GatewayErrorWebExceptionHandler.statusFor(
                new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Response took longer than 1000 ms",
                        new TimeoutException())))
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(GatewayErrorWebExceptionHandler.statusFor(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(GatewayErrorWebExceptionHandler.statusFor(
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY,
                        null, null)))
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(GatewayErrorWebExceptionHandler.statusFor(new IllegalStateException("boom")))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void bodyUsesFixedTextsAndNeverTheExceptionMessage() {
        MockServerWebExchange exchange = exchange("/api/v1/parcels/1");

        handler.handle(exchange, new ConnectException("Connection refused: parcel-service/10.0.0.5:8083")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"type\":\"https://swiftdrop.lk/problems/service-unavailable\"")
                .contains("\"title\":\"Service Unavailable\"")
                .contains("\"status\":503")
                .contains("\"detail\":\"Service temporarily unavailable\"")
                .contains("\"instance\":\"/api/v1/parcels/1\"")
                .contains("\"requestId\":\"req-1\"")
                .doesNotContain("parcel-service").doesNotContain("10.0.0.5").doesNotContain("8083");
    }

    @Test
    void unexpectedErrorIsGenericInternalError() {
        MockServerWebExchange exchange = exchange("/api/v1/parcels/1");

        handler.handle(exchange, new IllegalStateException("secret internals")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("An unexpected error occurred.")
                .doesNotContain("secret internals");
    }

    @Test
    void keepsRateLimitHeadersOnTooManyRequests() {
        MockServerWebExchange exchange = exchange("/api/v1/merchant-api/orders");
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", "0");

        handler.handle(exchange, HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "", HttpHeaders.EMPTY,
                null, null)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("too-many-requests");
    }

    @Test
    void otherStatusesUseTheReasonPhrase() {
        assertThat(GatewayErrorWebExceptionHandler.detailFor(HttpStatus.METHOD_NOT_ALLOWED))
                .isEqualTo("Method Not Allowed.");
    }

    @Test
    void committedResponseIsLeftAlone() {
        MockServerWebExchange exchange = exchange("/x");
        exchange.getResponse().setComplete().block();
        IllegalStateException error = new IllegalStateException("late");

        assertThatThrownBy(() -> handler.handle(exchange, error).block()).isSameAs(error);
    }

    private static MockServerWebExchange exchange(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        exchange.getAttributes().put(RequestIdWebFilter.ATTRIBUTE, "req-1");
        return exchange;
    }
}
