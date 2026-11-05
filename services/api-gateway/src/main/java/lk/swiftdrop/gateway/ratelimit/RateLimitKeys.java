package lk.swiftdrop.gateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Rate-limit bucket names. Each limiter has its own prefix so buckets never collide. */
final class RateLimitKeys {

    static final String MERCHANT_API_PREFIX = "merchant-api:";
    static final String PRICING_ESTIMATE_PREFIX = "pricing-estimate:";

    private RateLimitKeys() {
    }

    /** SHA-256 hex, so raw API keys and client IPs never reach Redis or logs. */
    static String sha256Hex(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Every JRE ships SHA-256
            throw new IllegalStateException(e);
        }
    }
}
