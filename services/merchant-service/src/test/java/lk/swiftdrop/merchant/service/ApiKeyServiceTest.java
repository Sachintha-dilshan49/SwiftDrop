package lk.swiftdrop.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.error.ForbiddenException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.merchant.dto.ApiKeyCreatedResponse;
import lk.swiftdrop.merchant.dto.ApiKeyResponse;
import lk.swiftdrop.merchant.dto.CreateApiKeyRequest;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantApiKey;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.repository.MerchantApiKeyRepository;
import lk.swiftdrop.merchant.repository.MerchantRepository;
import lk.swiftdrop.merchant.security.ApiKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private MerchantApiKeyRepository apiKeyRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private ApiKeyGenerator apiKeyGenerator;
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyGenerator = new ApiKeyGenerator();
        apiKeyService = new ApiKeyService(apiKeyRepository, merchantRepository, apiKeyGenerator);
    }

    @Test
    @DisplayName("Create API key fails if merchant status is PENDING")
    void createApiKeyFailsForPendingMerchant() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-1",
                "Nimal", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.PENDING);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> apiKeyService.createApiKey(merchantId, new CreateApiKeyRequest("Prod Key", null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only approved merchants can create API keys");
    }

    @Test
    @DisplayName("Create API key succeeds for APPROVED merchant and returns raw key")
    void createApiKeySuccess() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex Store", "BR-1",
                "Nimal", "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(apiKeyRepository.save(any(MerchantApiKey.class))).thenAnswer(inv -> {
            MerchantApiKey key = inv.getArgument(0);
            key.setId(UUID.randomUUID());
            key.setCreatedAt(Instant.now());
            return key;
        });

        ApiKeyCreatedResponse response = apiKeyService.createApiKey(
                merchantId, new CreateApiKeyRequest("Production Key", null));

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Production Key");
        assertThat(response.rawApiKey()).startsWith("sd_live_");
        assertThat(response.keyPrefix()).startsWith("sd_live_");
        verify(apiKeyRepository).save(any(MerchantApiKey.class));
    }

    @Test
    @DisplayName("List API keys returns only prefix and metadata, never raw secret or hash")
    void listApiKeys() {
        UUID merchantId = UUID.randomUUID();
        MerchantApiKey key = new MerchantApiKey(UUID.randomUUID(), merchantId, "Test Key",
                "sd_live_12345678", "hash123", null);
        key.setCreatedAt(Instant.now());

        when(apiKeyRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId))
                .thenReturn(List.of(key));

        List<ApiKeyResponse> keys = apiKeyService.listApiKeys(merchantId);
        assertThat(keys).hasSize(1);
        assertThat(keys.get(0).keyPrefix()).isEqualTo("sd_live_12345678");
        assertThat(keys.get(0).name()).isEqualTo("Test Key");
    }

    @Test
    @DisplayName("Revoke API key belonging to another merchant throws ForbiddenException")
    void revokeOtherMerchantKeyForbidden() {
        UUID merchantA = UUID.randomUUID();
        UUID merchantB = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        MerchantApiKey key = new MerchantApiKey(keyId, merchantB, "Test Key", "sd_live_123", "hash", null);
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.revokeApiKey(merchantA, keyId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot revoke API key belonging to another merchant");
    }

    @Test
    @DisplayName("Revoke API key marks key as revoked with timestamp")
    void revokeApiKeySuccess() {
        UUID merchantId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();

        MerchantApiKey key = new MerchantApiKey(keyId, merchantId, "Test Key", "sd_live_123", "hash", null);
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(MerchantApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        apiKeyService.revokeApiKey(merchantId, keyId);

        assertThat(key.isRevoked()).isTrue();
        assertThat(key.getRevokedAt()).isNotNull();
        verify(apiKeyRepository).save(key);
    }

    @Test
    @DisplayName("Validate API key returns empty when key is missing or blank")
    void validateBlankKey() {
        assertThat(apiKeyService.validateApiKey(null)).isEmpty();
        assertThat(apiKeyService.validateApiKey("   ")).isEmpty();
    }

    @Test
    @DisplayName("Validate API key returns empty when key hash is unknown")
    void validateUnknownKey() {
        String rawKey = apiKeyGenerator.generateRawApiKey();
        String hash = apiKeyGenerator.hashApiKey(rawKey);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.empty());

        assertThat(apiKeyService.validateApiKey(rawKey)).isEmpty();
    }

    @Test
    @DisplayName("Validate API key returns empty when key is revoked")
    void validateRevokedKey() {
        String rawKey = apiKeyGenerator.generateRawApiKey();
        String hash = apiKeyGenerator.hashApiKey(rawKey);
        UUID merchantId = UUID.randomUUID();

        MerchantApiKey key = new MerchantApiKey(UUID.randomUUID(), merchantId, "Key", "sd_live_1", hash, null);
        key.setRevoked(true);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));

        assertThat(apiKeyService.validateApiKey(rawKey)).isEmpty();
    }

    @Test
    @DisplayName("Validate API key returns empty when key is expired")
    void validateExpiredKey() {
        String rawKey = apiKeyGenerator.generateRawApiKey();
        String hash = apiKeyGenerator.hashApiKey(rawKey);
        UUID merchantId = UUID.randomUUID();

        MerchantApiKey key = new MerchantApiKey(UUID.randomUUID(), merchantId, "Key", "sd_live_1", hash,
                Instant.now().minus(1, ChronoUnit.DAYS));

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));

        assertThat(apiKeyService.validateApiKey(rawKey)).isEmpty();
    }

    @Test
    @DisplayName("Validate API key returns empty when associated merchant is not APPROVED")
    void validateKeyForUnapprovedMerchant() {
        String rawKey = apiKeyGenerator.generateRawApiKey();
        String hash = apiKeyGenerator.hashApiKey(rawKey);
        UUID merchantId = UUID.randomUUID();

        MerchantApiKey key = new MerchantApiKey(UUID.randomUUID(), merchantId, "Key", "sd_live_1", hash, null);
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.PENDING);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        assertThat(apiKeyService.validateApiKey(rawKey)).isEmpty();
    }

    @Test
    @DisplayName("Validate API key returns Merchant when key is valid and merchant is APPROVED")
    void validateKeySuccess() {
        String rawKey = apiKeyGenerator.generateRawApiKey();
        String hash = apiKeyGenerator.hashApiKey(rawKey);
        UUID merchantId = UUID.randomUUID();

        MerchantApiKey key = new MerchantApiKey(UUID.randomUUID(), merchantId, "Key", "sd_live_1", hash, null);
        Merchant merchant = new Merchant(merchantId, UUID.randomUUID(), "Apex", "BR-1", "Nimal",
                "nimal@apex.lk", "0771234567", "Colombo", MerchantStatus.APPROVED);

        when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(key));
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        Optional<Merchant> result = apiKeyService.validateApiKey(rawKey);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(merchantId);
    }
}
