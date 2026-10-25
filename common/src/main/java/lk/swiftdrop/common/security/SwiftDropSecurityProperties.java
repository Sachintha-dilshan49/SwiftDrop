package lk.swiftdrop.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code swiftdrop.security.*} settings.
 *
 * @param publicPaths extra path patterns that need no token, e.g. {@code /api/v1/track/**}.
 *                    Health and OpenAPI endpoints are always public.
 */
@ConfigurationProperties("swiftdrop.security")
public record SwiftDropSecurityProperties(List<String> publicPaths) {

    public SwiftDropSecurityProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
