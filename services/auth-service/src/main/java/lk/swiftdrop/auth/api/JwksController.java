package lk.swiftdrop.auth.api;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final RSAKey signingKey;

    public JwksController(RSAKey signingKey) {
        this.signingKey = signingKey;
    }

    @GetMapping(path = "/.well-known/jwks.json", produces = "application/json")
    public Map<String, Object> jwks() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }
}
