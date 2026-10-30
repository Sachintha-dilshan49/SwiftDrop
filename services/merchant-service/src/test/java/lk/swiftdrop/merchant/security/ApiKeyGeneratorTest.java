package lk.swiftdrop.merchant.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyGeneratorTest {

    private ApiKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ApiKeyGenerator();
    }

    @Test
    @DisplayName("Generated raw key starts with sd_live_ and has sufficient entropy")
    void generateRawApiKey() {
        String key1 = generator.generateRawApiKey();
        String key2 = generator.generateRawApiKey();

        assertThat(key1).startsWith("sd_live_");
        assertThat(key2).startsWith("sd_live_");
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.length()).isGreaterThan(30);
    }

    @Test
    @DisplayName("Extract prefix returns first 16 characters safely")
    void extractPrefix() {
        String key = "sd_live_abcdef1234567890extra";
        String prefix = generator.extractPrefix(key);

        assertThat(prefix).isEqualTo("sd_live_abcdef12");
        assertThat(generator.extractPrefix(null)).isEmpty();
    }

    @Test
    @DisplayName("Hash API key produces 64 character hex SHA-256 hash")
    void hashApiKey() {
        String rawKey = "sd_live_test_key_12345";
        String hash1 = generator.hashApiKey(rawKey);
        String hash2 = generator.hashApiKey(rawKey);

        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(generator.hashApiKey("sd_live_different_key"));
    }

    @Test
    @DisplayName("Constant time equality works correctly")
    void constantTimeEquals() {
        String hash = generator.hashApiKey("sd_live_secret");
        String same = generator.hashApiKey("sd_live_secret");
        String different = generator.hashApiKey("sd_live_other");

        assertThat(generator.constantTimeEquals(hash, same)).isTrue();
        assertThat(generator.constantTimeEquals(hash, different)).isFalse();
        assertThat(generator.constantTimeEquals(hash, null)).isFalse();
        assertThat(generator.constantTimeEquals(null, hash)).isFalse();
    }

    @Test
    @DisplayName("Generated webhook secret starts with whsec_ and has sufficient entropy")
    void generateWebhookSecret() {
        String secret1 = generator.generateWebhookSecret();
        String secret2 = generator.generateWebhookSecret();

        assertThat(secret1).startsWith("whsec_");
        assertThat(secret2).startsWith("whsec_");
        assertThat(secret1).isNotEqualTo(secret2);
    }
}
