package lk.swiftdrop.merchant.service;

import java.util.UUID;
import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.merchant.dto.MerchantResponse;
import lk.swiftdrop.merchant.dto.RegisterMerchantRequest;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public MerchantResponse register(RegisterMerchantRequest request, UUID userId) {
        if (merchantRepository.existsByBusinessRegNo(request.businessRegNo())) {
            throw new ConflictException("Business registration number is already registered");
        }
        if (merchantRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }
        if (userId != null && merchantRepository.existsByUserId(userId)) {
            throw new ConflictException("User already has a registered merchant account");
        }

        Merchant merchant = new Merchant(
                UUID.randomUUID(),
                userId,
                request.businessName(),
                request.businessRegNo(),
                request.contactName(),
                request.email(),
                request.phone(),
                request.address(),
                MerchantStatus.PENDING
        );

        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant registered successfully with id: {}, status: {}", saved.getId(), saved.getStatus());
        return MerchantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchantById(UUID id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));
        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public Merchant findEntityById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));
    }

    @Transactional(readOnly = true)
    public MerchantResponse getMerchantByUserId(UUID userId) {
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Merchant profile not found for user: " + userId));
        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public Merchant findEntityByUserId(UUID userId) {
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Merchant profile not found for user: " + userId));
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> listMerchantsByStatus(MerchantStatus status, Pageable pageable) {
        if (status != null) {
            return merchantRepository.findAllByStatus(status, pageable)
                    .map(MerchantResponse::from);
        }
        return merchantRepository.findAll(pageable)
                .map(MerchantResponse::from);
    }

    @Transactional
    public MerchantResponse approveMerchant(UUID id) {
        Merchant merchant = findEntityById(id);
        merchant.setStatus(MerchantStatus.APPROVED);
        merchant.setRejectionReason(null);
        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant {} approved by admin", id);
        return MerchantResponse.from(saved);
    }

    @Transactional
    public MerchantResponse rejectMerchant(UUID id, String reason) {
        Merchant merchant = findEntityById(id);
        merchant.setStatus(MerchantStatus.REJECTED);
        merchant.setRejectionReason(reason);
        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant {} rejected by admin", id);
        return MerchantResponse.from(saved);
    }
}
