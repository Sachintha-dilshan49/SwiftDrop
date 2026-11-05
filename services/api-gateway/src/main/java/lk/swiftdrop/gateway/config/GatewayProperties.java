package lk.swiftdrop.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway settings under {@code swiftdrop.gateway}.
 *
 * @param publicPaths requests that need no JWT; {@code method} is optional (any method when absent)
 * @param cors        browser origins allowed to call the API
 */
@ConfigurationProperties("swiftdrop.gateway")
public record GatewayProperties(List<PublicPath> publicPaths, Cors cors) {

    public GatewayProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
        cors = cors == null ? new Cors(List.of()) : cors;
    }

    public record PublicPath(String method, String path) {
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }
}
