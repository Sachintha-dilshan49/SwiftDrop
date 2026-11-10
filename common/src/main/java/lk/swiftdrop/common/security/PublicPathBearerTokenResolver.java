package lk.swiftdrop.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Reads the bearer token like {@link DefaultBearerTokenResolver}, except on public paths, where it reports
 * no token. Without this, {@code BearerTokenAuthenticationFilter} rejects an expired or invalid token with 401
 * before the {@code permitAll} rule for the public path is ever checked.
 *
 * <p>The cost: a public endpoint never sees the caller's identity, even when the token is valid.
 */
public class PublicPathBearerTokenResolver implements BearerTokenResolver {

    private final RequestMatcher publicPaths;
    private final BearerTokenResolver delegate;

    public PublicPathBearerTokenResolver(RequestMatcher publicPaths) {
        this(publicPaths, new DefaultBearerTokenResolver());
    }

    public PublicPathBearerTokenResolver(RequestMatcher publicPaths, BearerTokenResolver delegate) {
        this.publicPaths = Objects.requireNonNull(publicPaths, "publicPaths");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public String resolve(HttpServletRequest request) {
        return publicPaths.matches(request) ? null : delegate.resolve(request);
    }
}
