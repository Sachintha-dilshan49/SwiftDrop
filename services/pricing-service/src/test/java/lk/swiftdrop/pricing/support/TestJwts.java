package lk.swiftdrop.pricing.support;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Makes a mocked {@link JwtDecoder} accept {@code <role>-token} for every role (e.g. {@code admin-token}) and
 * reject anything else as an invalid token.
 */
public final class TestJwts {

    public static final List<String> ROLES = List.of("CUSTOMER", "MERCHANT", "COURIER", "DISPATCHER", "ADMIN");

    private TestJwts() {
    }

    public static void acceptRoleTokens(JwtDecoder jwtDecoder) {
        given(jwtDecoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            String role = ROLES.stream()
                    .filter(candidate -> token.equals(tokenFor(candidate)))
                    .findFirst()
                    .orElseThrow(() -> new BadJwtException("Invalid token"));
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .subject(UUID.randomUUID().toString())
                    .claim("roles", List.of(role))
                    .build();
        });
    }

    public static String tokenFor(String role) {
        return role.toLowerCase() + "-token";
    }

    public static String bearer(String role) {
        return "Bearer " + tokenFor(role);
    }
}
