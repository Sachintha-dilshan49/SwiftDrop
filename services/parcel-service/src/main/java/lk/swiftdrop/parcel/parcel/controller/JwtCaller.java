package lk.swiftdrop.parcel.parcel.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Reads the caller's identity from a verified user access token. */
final class JwtCaller {

    private JwtCaller() {
    }

    /** The user ID in {@code sub}; a token without a UUID subject is rejected with 401. */
    static UUID userId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new InvalidTokenException();
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }
}
