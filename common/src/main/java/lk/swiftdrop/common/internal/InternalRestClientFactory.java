package lk.swiftdrop.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ProblemDetails;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.SwiftDropException;
import lk.swiftdrop.common.web.RequestIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates {@link RestClient}s for calling another SwiftDrop service directly (ADR-010):
 *
 * <ul>
 *   <li>adds {@code X-Internal-Token} and the current {@code X-Request-Id}</li>
 *   <li>2 s connect and 5 s read timeouts</li>
 *   <li>maps errors so the shared handler answers the original client sensibly:
 *       404 → {@link NotFoundException}, 409 → {@link ConflictException} (both keep the callee's
 *       {@code type}, {@code title} and {@code detail}), 5xx, timeouts and connection failures →
 *       {@link ServiceUnavailableException} (503). Other 4xx stay Spring's
 *       {@code HttpClientErrorException}: they mean the caller sent a bad request, which is a bug (500).</li>
 * </ul>
 *
 * <pre>{@code
 * @Bean
 * RestClient authClient(InternalRestClientFactory clients,
 *                       @Value("${swiftdrop.services.auth-url}") String authUrl) {   // AUTH_SERVICE_URL
 *     return clients.create("auth-service", authUrl);
 * }
 *
 * // in a service class; a 404 from auth-service arrives here as NotFoundException
 * UserContact contact = authClient.get()
 *         .uri("/internal/users/{id}/contact", customerId)
 *         .retrieve()
 *         .body(UserContact.class);
 * }</pre>
 */
public class InternalRestClientFactory {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private static final Logger log = LoggerFactory.getLogger(InternalRestClientFactory.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int MAX_PROBLEM_BYTES = 8 * 1024;

    private final String token;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public InternalRestClientFactory(String token) {
        this(token, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    InternalRestClientFactory(String token, Duration connectTimeout, Duration readTimeout) {
        this.token = token;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * @param serviceName used in error messages and logs, e.g. {@code pricing-service}
     * @param baseUrl     e.g. {@code http://pricing-service:8082}, from {@code PRICING_SERVICE_URL}
     */
    public RestClient create(String serviceName, String baseUrl) {
        return builder(serviceName, baseUrl).build();
    }

    /** Same as {@link #create} but returns the builder, e.g. to add a message converter or bind a mock server. */
    public RestClient.Builder builder(String serviceName, String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new InternalCallInterceptor(serviceName, token))
                .defaultStatusHandler(status -> status.value() == 404 || status.value() == 409,
                        (request, response) -> {
                            throw mapClientError(serviceName, response);
                        })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.warn("{} answered {} for {} {}", serviceName, response.getStatusCode().value(),
                            request.getMethod(), request.getURI().getPath());
                    throw unavailable(serviceName, null);
                });
    }

    private static SwiftDropException mapClientError(String serviceName, ClientHttpResponse response)
            throws IOException {
        Problem problem = Problem.read(response);
        if (response.getStatusCode().value() == 404) {
            return problem == null
                    ? new NotFoundException(serviceName + " has no such resource.")
                    : new NotFoundException(problem.type("not-found"), problem.title("Resource not found"),
                            problem.detail());
        }
        return problem == null
                ? new ConflictException(serviceName + " rejected the request because of a conflict.")
                : new ConflictException(problem.type("conflict"), problem.title("Conflict"), problem.detail());
    }

    private static ServiceUnavailableException unavailable(String serviceName, Throwable cause) {
        return new ServiceUnavailableException(serviceName + " is not available right now. Please try again later.",
                cause);
    }

    /** Adds the headers and turns I/O failures (timeouts, refused connections) into 503. */
    private record InternalCallInterceptor(String serviceName, String token) implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
            request.getHeaders().set(InternalTokenAuthenticationFilter.HEADER, token);
            String requestId = RequestIds.current();
            if (requestId != null) {
                request.getHeaders().set(RequestIds.HEADER, requestId);
            }
            try {
                return execution.execute(request, body);
            } catch (IOException ex) {
                log.warn("Call to {} {} {} failed: {}", serviceName, request.getMethod(), request.getURI().getPath(),
                        ex.toString());
                throw unavailable(serviceName, ex);
            }
        }

        @Override
        public String toString() {
            return "InternalCallInterceptor[serviceName=" + serviceName + ", token=****]";
        }
    }

    /** The parts of the callee's ProblemDetail worth passing on; its detail is client-safe by convention. */
    private record Problem(String type, String title, String detail) {

        static Problem read(ClientHttpResponse response) {
            try (InputStream body = response.getBody()) {
                JsonNode node = JSON.readTree(body.readNBytes(MAX_PROBLEM_BYTES));
                if (node == null || !node.isObject() || !node.hasNonNull("detail")) {
                    return null;
                }
                return new Problem(text(node, "type"), text(node, "title"), text(node, "detail"));
            } catch (IOException | RuntimeException notAProblem) {
                return null;
            }
        }

        String type(String fallback) {
            return type != null && type.startsWith(ProblemDetails.TYPE_BASE)
                    ? type.substring(ProblemDetails.TYPE_BASE.length())
                    : fallback;
        }

        String title(String fallback) {
            return title != null ? title : fallback;
        }

        private static String text(JsonNode node, String field) {
            JsonNode value = node.get(field);
            return value == null || value.isNull() ? null : value.asString();
        }
    }
}
