package lk.swiftdrop.merchant.service;

import java.time.Instant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final MerchantApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    public ApiKeyService(MerchantApiKeyRepository apiKeyRepository,
                         MerchantRepository merchantRepository,
                         ApiKeyGenerator apiKeyGenerator) {
        this.apiKeyRepository = apiKeyRepository;
        this.merchantRepository = merchantRepository;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    @Transactional
    public ApiKeyCreatedResponse createApiKey(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + merchantId));

        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            throw new ForbiddenException("Only approved merchants can create API keys");
        }

        String rawKey = apiKeyGenerator.generateRawApiKey();
        String prefix = apiKeyGenerator.extractPrefix(rawKey);
        String hash = apiKeyGenerator.hashApiKey(rawKey);

        MerchantApiKey apiKey = new MerchantApiKey(
                UUID.randomUUID(),
                merchantId,
                request.name(),
                prefix,
                hash,
                request.expiresAt()
        );

        MerchantApiKey saved = apiKeyRepository.save(apiKey);
        log.info("API key created for merchantId: {}, keyId: {}, prefix: {}", merchantId, saved.getId(), prefix);

        return new ApiKeyCreatedResponse(
                saved.getId(),
                saved.getName(),
                saved.getKeyPrefix(),
                rawKey,
                saved.getExpiresAt(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID merchantId) {
        return apiKeyRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revokeApiKey(UUID merchantId, UUID keyId) {
        MerchantApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new NotFoundException("API key not found: " + keyId));

        if (!apiKey.getMerchantId().equals(merchantId)) {
            throw new ForbiddenException("Cannot revoke API key belonging to another merchant");
        }

        if (apiKey.isRevoked()) {
            return;
        }

        apiKey.setRevoked(true);
        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);
        log.info("API key {} revoked for merchantId {}", keyId, merchantId);
    }

    @Transactional(readOnly = true)
    public Optional<Merchant> validateApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }

        String hash = apiKeyGenerator.hashApiKey(rawApiKey);
        Optional<MerchantApiKey> keyOpt = apiKeyRepository.findByKeyHash(hash);
        if (keyOpt.isEmpty()) {
            return Optional.empty();
        }

        MerchantApiKey key = keyOpt.get();
        if (key.isRevoked()) {
            return Optional.empty();
        }

        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }

        if (!apiKeyGenerator.constantTimeEquals(key.getKeyHash(), hash)) {
            return Optional.empty();
        }

        Optional<Merchant> merchantOpt = merchantRepository.findById(key.getMerchantId());
        if (merchantOpt.isEmpty()) {
            return Optional.empty();
        }

        Merchant merchant = merchantOpt.get();
        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            log.warn("Merchant {} is not APPROVED (status: {}), rejecting API key auth", merchant.getId(), merchant.getStatus());
            return Optional.empty();
        }

        return Optional.of(merchant);
    }
}
