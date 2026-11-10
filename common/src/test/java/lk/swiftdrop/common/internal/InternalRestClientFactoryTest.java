package lk.swiftdrop.common.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.web.RequestIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class InternalRestClientFactoryTest {

    private static final String BASE = "http://parcel-service:8083";

    private final InternalRestClientFactory factory = new InternalRestClientFactory("shared-token");
    private final RestClient.Builder builder = factory.builder("parcel-service", BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final RestClient client = builder.build();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void sendsInternalTokenAndCurrentRequestId() {
        MDC.put(RequestIds.MDC_KEY, "req-42");
        server.expect(requestTo(BASE + "/internal/parcels/1/contacts"))
                .andExpect(header(InternalTokenAuthenticationFilter.HEADER, "shared-token"))
                .andExpect(header(RequestIds.HEADER, "req-42"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        String body = client.get().uri("/internal/parcels/1/contacts").retrieve().body(String.class);

        assertThat(body).contains("ok");
        server.verify();
    }

    @Test
    void omitsRequestIdOutsideARequest() {
        server.expect(requestTo(BASE + "/internal/x"))
                .andExpect(header(InternalTokenAuthenticationFilter.HEADER, "shared-token"))
                .andExpect(headerDoesNotExist(RequestIds.HEADER))
                .andRespond(withSuccess());

        client.get().uri("/internal/x").retrieve().toBodilessEntity();

        server.verify();
    }

    @Test
    void maps404ToNotFoundKeepingTheCalleesProblem() {
        server.expect(requestTo(BASE + "/internal/parcels/9/contacts"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"type":"https://swiftdrop.lk/problems/parcel-not-found","title":"Parcel not found",
                                 "status":404,"detail":"Parcel 9 does not exist."}"""));

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> client.get().uri("/internal/parcels/9/contacts").retrieve().body(String.class))
                .withMessage("Parcel 9 does not exist.")
                .satisfies(ex -> {
                    assertThat(ex.getProblemType()).isEqualTo("parcel-not-found");
                    assertThat(ex.getTitle()).isEqualTo("Parcel not found");
                });
    }

    @Test
    void maps404WithoutProblemBodyToGenericNotFound() {
        server.expect(requestTo(BASE + "/internal/x")).andRespond(withStatus(HttpStatus.NOT_FOUND).body("<html/>"));

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> client.get().uri("/internal/x").retrieve().toBodilessEntity())
                .withMessage("parcel-service has no such resource.")
                .satisfies(ex -> assertThat(ex.getProblemType()).isEqualTo("not-found"));
    }

    @Test
    void maps409ToConflictKeepingTheCalleesProblem() {
        server.expect(requestTo(BASE + "/internal/parcels/1/status"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"type":"https://swiftdrop.lk/problems/invalid-status-transition",
                                 "title":"Invalid status transition","status":409,
                                 "detail":"Parcel cannot move from CREATED to DELIVERED."}"""));

        assertThatExceptionOfType(ConflictException.class)
                .isThrownBy(() -> client.patch().uri("/internal/parcels/1/status").retrieve().toBodilessEntity())
                .withMessage("Parcel cannot move from CREATED to DELIVERED.")
                .satisfies(ex -> assertThat(ex.getProblemType()).isEqualTo("invalid-status-transition"));
    }

    @Test
    void maps5xxToServiceUnavailable() {
        server.expect(requestTo(BASE + "/internal/x")).andRespond(withServerError());

        assertThatExceptionOfType(ServiceUnavailableException.class)
                .isThrownBy(() -> client.get().uri("/internal/x").retrieve().toBodilessEntity())
                .withMessageContaining("parcel-service is not available")
                .satisfies(ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void mapsTimeoutToServiceUnavailable() {
        server.expect(requestTo(BASE + "/internal/x")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatExceptionOfType(ServiceUnavailableException.class)
                .isThrownBy(() -> client.get().uri("/internal/x").retrieve().toBodilessEntity())
                .withCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void leavesOther4xxAsClientErrors() {
        server.expect(requestTo(BASE + "/internal/x")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> client.get().uri("/internal/x").retrieve().toBodilessEntity());
    }

    @Test
    void appliesTheReadTimeoutToRealConnections() throws Exception {
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/internal/slow", exchange -> {
            try {
                Thread.sleep(2_000);
                exchange.sendResponseHeaders(204, -1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        slow.start();
        try {
            RestClient shortTimeouts = new InternalRestClientFactory("shared-token", Duration.ofSeconds(1),
                    Duration.ofMillis(200)).create("slow-service", "http://127.0.0.1:" + slow.getAddress().getPort());

            assertThatExceptionOfType(ServiceUnavailableException.class)
                    .isThrownBy(() -> shortTimeouts.get().uri("/internal/slow").retrieve().toBodilessEntity())
                    .withCauseInstanceOf(SocketTimeoutException.class);
        } finally {
            slow.stop(0);
        }
    }

    @Test
    void usesTheDocumentedDefaultTimeouts() {
        assertThat(InternalRestClientFactory.CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(2));
        assertThat(InternalRestClientFactory.READ_TIMEOUT).isEqualTo(Duration.ofSeconds(5));
    }
}
