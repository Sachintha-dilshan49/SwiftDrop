package lk.swiftdrop.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("swiftdrop.jwt")
public class JwtProperties {

    private String privateKeyBase64;
    private String keyId = "swiftdrop-auth-1";
    private String issuer = "swiftdrop-auth";

    public String getPrivateKeyBase64() {
        return privateKeyBase64;
    }

    public void setPrivateKeyBase64(String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
