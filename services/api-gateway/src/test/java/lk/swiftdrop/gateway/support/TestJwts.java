package lk.swiftdrop.gateway.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** RS256 test tokens, signed with a key whose public half WireMock serves as auth-service's JWKS. */
public final class TestJwts {

    private static final String KEY_ID = "test-key-1";

    private final RSAKey signingKey;
    private final RSAKey otherKey;

    public TestJwts() {
        try {
            signingKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
            // Same kid, different key: a token signed with it must fail signature verification
            otherKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public String jwksJson() {
        return new JWKSet(signingKey.toPublicJWK()).toString();
    }

    public String valid() {
        return sign(signingKey, Instant.now().plus(Duration.ofMinutes(15)));
    }

    public String expired() {
        return sign(signingKey, Instant.now().minus(Duration.ofMinutes(10)));
    }

    public String wrongSignature() {
        return sign(otherKey, Instant.now().plus(Duration.ofMinutes(15)));
    }

    private static String sign(RSAKey key, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("swiftdrop-auth")
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("CUSTOMER"))
                .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(15))))
                .expirationTime(Date.from(expiresAt))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
