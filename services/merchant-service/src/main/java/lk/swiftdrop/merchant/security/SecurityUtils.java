package lk.swiftdrop.merchant.security;

import java.util.UUID;
import lk.swiftdrop.common.error.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ForbiddenException("Authentication required");
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                throw new ForbiddenException("Invalid user ID in token subject");
            }
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Unable to determine user ID from authentication");
        }
    }

    public static UUID getOptionalAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return UUID.fromString(jwt.getSubject());
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (Exception ignored) {
            return null;
        }
    }
}
