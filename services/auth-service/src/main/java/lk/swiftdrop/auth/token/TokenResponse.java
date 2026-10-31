package lk.swiftdrop.auth.token;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UserSummary user) {

    public TokenResponse(String accessToken, Instant accessTokenExpiresAt, String refreshToken, UserSummary user) {
        this(accessToken, "Bearer", accessTokenExpiresAt, refreshToken, user);
    }

    public record UserSummary(UUID id, String fullName, String email, List<String> roles) {
    }
}
