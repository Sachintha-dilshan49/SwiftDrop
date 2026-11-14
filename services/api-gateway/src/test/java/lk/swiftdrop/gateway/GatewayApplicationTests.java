package lk.swiftdrop.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

/**
 * Starts without auth-service, Redis or Docker: the JWKS is fetched on the first token and
 * Redis connects on the first rate-limited request.
 */
@SpringBootTest
class GatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoadsWithoutDownstreamServicesOrRedis() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes).extracting(Route::getId).containsExactly(
                "parcel-tracking-stream",
                "merchant-api", "pricing-estimate", "assistant-chat", "assistant-mcp", "auth-admin", "pricing-admin", "merchant-admin",
                "notification-admin",
                "auth-service", "auth-google-oauth", "pricing-service", "parcel-service", "dispatch-service",
                "merchant-service", "ai-assistant");
        assertThat(routes).filteredOn(route -> route.getId().equals("parcel-tracking-stream"))
                .singleElement()
                .satisfies(route -> assertThat(route.getMetadata()).containsEntry("response-timeout", -1));
        assertThat(routes).filteredOn(route -> route.getId().equals("assistant-chat"))
                .singleElement()
                .satisfies(route -> assertThat(route.getMetadata()).containsEntry("response-timeout", 120000));
    }
}
