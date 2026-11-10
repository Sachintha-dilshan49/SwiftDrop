package lk.swiftdrop.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import lk.swiftdrop.common.error.ValidationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfiguration {

    @Bean
    RSAKey signingKey(JwtProperties properties) {
        if (properties.getPrivateKeyBase64() == null || properties.getPrivateKeyBase64().isBlank()) {
            throw new IllegalStateException("JWT_PRIVATE_KEY_B64 must contain a base64-encoded PKCS#8 RSA private key.");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(properties.getPrivateKeyBase64().trim());
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(properties.getKeyId())
                    .build();
        } catch (IllegalArgumentException | java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("JWT_PRIVATE_KEY_B64 is not a valid PKCS#8 RSA private key.", exception);
        }
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey signingKey) {
        return new ImmutableJWKSet<>(new JWKSet(signingKey));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}
