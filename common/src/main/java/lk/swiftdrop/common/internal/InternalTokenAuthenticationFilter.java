package lk.swiftdrop.common.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates {@code /internal/**} callers that send the shared {@code X-Internal-Token}.
 * A match gives {@link #ROLE}; anything else leaves the request unauthenticated so the
 * internal filter chain answers 401. The token value is never logged.
 */
public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";
    public static final String ROLE = "ROLE_INTERNAL_SERVICE";
    static final String PRINCIPAL = "internal-service";

    private static final Logger log = LoggerFactory.getLogger(InternalTokenAuthenticationFilter.class);

    private final byte[] expectedDigest;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public InternalTokenAuthenticationFilter(String expectedToken) {
        this.expectedDigest = sha256(expectedToken);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        if (presented != null && matches(presented)) {
            SecurityContext context = contextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, null,
                    List.of(new SimpleGrantedAuthority(ROLE))));
            contextHolder.setContext(context);
        } else {
            log.warn("Rejected internal call to {} {}: {} {}", request.getMethod(), request.getRequestURI(),
                    presented == null ? "missing" : "invalid", HEADER);
        }
        chain.doFilter(request, response);
    }

    // Hashing first makes the comparison independent of both content and length
    private boolean matches(String presented) {
        return MessageDigest.isEqual(expectedDigest, sha256(presented));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
