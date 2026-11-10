package lk.swiftdrop.pricing.promo.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.FieldErrorDetail;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.dto.CreatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.dto.PromoCodeResponse;
import lk.swiftdrop.pricing.promo.dto.UpdatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.promo.repository.PromoCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Promo code management for admins. Deleting a code removes it; parcels keep the discount they were given, so
 * nothing else refers to it.
 */
@Service
public class PromoCodeService {

    private static final Logger log = LoggerFactory.getLogger(PromoCodeService.class);

    private final PromoCodeRepository promoCodeRepository;

    public PromoCodeService(PromoCodeRepository promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> allPromoCodes() {
        return promoCodeRepository.findAllByOrderByCodeAsc().stream().map(PromoCodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PromoCodeResponse promoCode(UUID id) {
        return PromoCodeResponse.from(find(id));
    }

    @Transactional
    public PromoCodeResponse create(CreatePromoCodeRequest request) {
        requireValidPeriod(request.validFrom(), request.validTo());
        String code = request.code().toUpperCase(Locale.ROOT);
        if (promoCodeRepository.existsByCode(code)) {
            throw new ConflictException("Promo code " + code + " already exists.");
        }
        PromoCode promo = new PromoCode(code, request.discountAmount(), request.validFrom(), request.validTo(),
                request.active() == null || request.active());
        promoCodeRepository.saveAndFlush(promo);
        log.info("Promo code {} created", code);
        return PromoCodeResponse.from(promo);
    }

    @Transactional
    public PromoCodeResponse update(UUID id, UpdatePromoCodeRequest request) {
        requireValidPeriod(request.validFrom(), request.validTo());
        PromoCode promo = find(id);
        promo.update(request.discountAmount(), request.validFrom(), request.validTo(), request.active());
        promoCodeRepository.flush();
        log.info("Promo code {} updated, active={}", promo.getCode(), promo.isActive());
        return PromoCodeResponse.from(promo);
    }

    @Transactional
    public void delete(UUID id) {
        PromoCode promo = find(id);
        promoCodeRepository.delete(promo);
        log.info("Promo code {} deleted", promo.getCode());
    }

    private PromoCode find(UUID id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo code " + id + " was not found."));
    }

    private static void requireValidPeriod(Instant validFrom, Instant validTo) {
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new ValidationException("The end of the validity period must be after its start.",
                    List.of(new FieldErrorDetail("validTo", "must be after validFrom")));
        }
    }
}
