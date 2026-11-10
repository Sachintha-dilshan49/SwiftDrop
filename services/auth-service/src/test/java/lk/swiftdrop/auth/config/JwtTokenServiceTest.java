package lk.swiftdrop.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lk.swiftdrop.auth.account.RoleEntity;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.account.UserEntity;
import lk.swiftdrop.auth.token.JwtTokenService;
import lk.swiftdrop.auth.token.TokenResponse;
import lk.swiftdrop.auth.token.TokenResponse.UserSummary;
import lk.swiftdrop.auth.api.JwksController;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");

    @Test
    void createsRs256AccessTokenWithExpectedClaimsAndExpiry() throws Exception {
        KeyPair pair = rsaKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) pair.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey) pair.getPrivate())
                .keyID("test-key")
                .build();
        JwtTokenService service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey))),
                new JwtProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        UserEntity user = new UserEntity("Demo Customer", "demo@example.test", null, "hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.addRole(new RoleEntity(RoleName.CUSTOMER));

        String encoded = service.createAccessToken(user);
        SignedJWT decoded = SignedJWT.parse(encoded);
        assertThat(decoded.verify(new RSASSAVerifier((java.security.interfaces.RSAPublicKey) pair.getPublic()))).isTrue();
        assertThat(decoded.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(decoded.getJWTClaimsSet().getSubject()).isEqualTo(user.getId().toString());
        assertThat(decoded.getJWTClaimsSet().getIssuer()).isEqualTo("swiftdrop-auth");
        assertThat(decoded.getJWTClaimsSet().getIssueTime().toInstant()).isEqualTo(NOW);
        assertThat(decoded.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(NOW.plusSeconds(900));
        assertThat(decoded.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("CUSTOMER");
    }

    @Test
    void createsRandomRefreshTokensAndDeterministicSha256Hashes() {
        JwtTokenService service = new JwtTokenService(null, new JwtProperties(), Clock.systemUTC());
        String first = service.createRefreshToken();
        String second = service.createRefreshToken();

        assertThat(first).hasSize(43).isNotEqualTo(second);
        assertThat(service.hashRefreshToken(first))
                .hasSize(64)
                .isEqualTo(service.hashRefreshToken(first))
                .isNotEqualTo(service.hashRefreshToken(second));
    }

    @Test
    void buildsSigningKeyAndPublicOnlyJwksFromPkcs8PrivateKey() throws Exception {
        KeyPair pair = rsaKeyPair();
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyBase64(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        properties.setKeyId("demo-key");
        JwtKeyConfiguration configuration = new JwtKeyConfiguration();
        RSAKey signingKey = configuration.signingKey(properties);

        assertThat(signingKey.getKeyID()).isEqualTo("demo-key");
        assertThat(signingKey.isPrivate()).isTrue();
        var jwks = new JwksController(signingKey).jwks();
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> keys = (List<java.util.Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst()).containsEntry("kid", "demo-key").doesNotContainKey("d");
        assertThat(configuration.jwkSource(signingKey)).isNotNull();
        assertThat(configuration.jwtEncoder(configuration.jwkSource(signingKey))).isInstanceOf(NimbusJwtEncoder.class);
    }

    @Test
    void rejectsMissingOrMalformedSigningKey() {
        JwtKeyConfiguration configuration = new JwtKeyConfiguration();
        assertThatThrownBy(() -> configuration.signingKey(new JwtProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY_B64");

        JwtProperties invalid = new JwtProperties();
        invalid.setPrivateKeyBase64("%%%");
        assertThatThrownBy(() -> configuration.signingKey(invalid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid PKCS#8 RSA private key");
    }

    @Test
    void exposesConfigPropertyAccessorsAndAuthBeans() {
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyBase64("private-key");
        properties.setKeyId("key-id");
        properties.setIssuer("issuer");
        assertThat(properties.getPrivateKeyBase64()).isEqualTo("private-key");
        assertThat(properties.getKeyId()).isEqualTo("key-id");
        assertThat(properties.getIssuer()).isEqualTo("issuer");

        AuthConfiguration configuration = new AuthConfiguration();
        String encodedPassword = configuration.passwordEncoder().encode("password");
        assertThat(encodedPassword).startsWith("$2a$12$");
        assertThat(configuration.passwordEncoder().matches("password", encodedPassword)).isTrue();
        assertThat(configuration.clock().getZone()).isEqualTo(ZoneOffset.UTC);

        TokenResponse response = new TokenResponse("access", NOW, "refresh",
                new UserSummary(UUID.randomUUID(), "Demo", "demo@example.test", List.of("CUSTOMER")));
        assertThat(response.user().fullName()).isEqualTo("Demo");
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
