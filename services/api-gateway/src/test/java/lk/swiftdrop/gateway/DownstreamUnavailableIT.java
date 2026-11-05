package lk.swiftdrop.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Every service URL points at a closed port, as if the services were down. Uses a public path,
 * so neither the JWKS nor Redis is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamUnavailableIT {

    private static final int CLOSED_PORT = closedPort();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void deadServices(DynamicPropertyRegistry registry) {
        for (String name : new String[] {"AUTH_SERVICE_URL", "PRICING_SERVICE_URL", "PARCEL_SERVICE_URL",
                "DISPATCH_SERVICE_URL", "MERCHANT_SERVICE_URL", "NOTIFICATION_SERVICE_URL", "AI_ASSISTANT_URL"}) {
            registry.add(name, () -> "http://127.0.0.1:" + CLOSED_PORT);
        }
    }

    @Test
    void unreachableServiceIsServiceUnavailableProblemWithoutHostDetails() {
        String body = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10)).build()
                .get().uri("/api/v1/track/SD-1")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists("X-Request-Id")
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(body)
                .contains("\"type\":\"https://swiftdrop.lk/problems/service-unavailable\"")
                .contains("\"title\":\"Service Unavailable\"")
                .contains("\"detail\":\"Service temporarily unavailable\"")
                .contains("\"instance\":\"/api/v1/track/SD-1\"")
                .contains("\"requestId\":")
                .doesNotContain("127.0.0.1")
                .doesNotContain(String.valueOf(CLOSED_PORT))
                .doesNotContain("Connection refused");
    }

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
