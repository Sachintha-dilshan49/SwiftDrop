package lk.swiftdrop.pricing.promo.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.dto.CreatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.dto.PromoCodeResponse;
import lk.swiftdrop.pricing.promo.dto.UpdatePromoCodeRequest;
import lk.swiftdrop.pricing.promo.entity.PromoCode;
import lk.swiftdrop.pricing.promo.repository.PromoCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static lk.swiftdrop.pricing.support.PricingFixtures.drop100;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    private static final Instant FROM = Instant.parse("2026-12-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoCodeService service;

    @Test
    void storesTheCodeInUpperCaseAndActiveByDefault() {
        given(promoCodeRepository.existsByCode("XMAS50")).willReturn(false);

        PromoCodeResponse created = service.create(
                new CreatePromoCodeRequest("xmas50", new BigDecimal("50.00"), FROM, TO, null));

        assertThat(created.code()).isEqualTo("XMAS50");
        assertThat(created.active()).isTrue();
        assertThat(created.currency()).isEqualTo("LKR");
        verify(promoCodeRepository).saveAndFlush(any(PromoCode.class));
    }

    @Test
    void codeWithoutEndDateIsAccepted() {
        PromoCodeResponse created = service.create(
                new CreatePromoCodeRequest("FOREVER", new BigDecimal("10.00"), FROM, null, false));

        assertThat(created.validTo()).isNull();
        assertThat(created.active()).isFalse();
    }

    @Test
    void duplicateCodeIsAConflictWhateverItsCase() {
        given(promoCodeRepository.existsByCode("DROP100")).willReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreatePromoCodeRequest("drop100", new BigDecimal("50.00"), FROM, null, true)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Promo code DROP100 already exists.");
        verify(promoCodeRepository, never()).saveAndFlush(any());
    }

    @Test
    void periodMustEndAfterItStarts() {
        assertThatThrownBy(() -> service.create(
                new CreatePromoCodeRequest("BAD", new BigDecimal("50.00"), TO, FROM, true)))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                        .extracting("field").containsExactly("validTo"));
        assertThatThrownBy(() -> service.update(UUID.randomUUID(),
                new UpdatePromoCodeRequest(new BigDecimal("50.00"), FROM, FROM, true)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updatesAmountPeriodAndActiveFlag() {
        PromoCode promo = drop100();
        given(promoCodeRepository.findById(promo.getId())).willReturn(Optional.of(promo));

        PromoCodeResponse updated = service.update(promo.getId(),
                new UpdatePromoCodeRequest(new BigDecimal("150.00"), FROM, TO, false));

        assertThat(updated.code()).isEqualTo("DROP100");
        assertThat(updated.discountAmount()).isEqualByComparingTo("150.00");
        assertThat(updated.validTo()).isEqualTo(TO);
        assertThat(updated.active()).isFalse();
    }

    @Test
    void deletesThePromoCode() {
        PromoCode promo = drop100();
        given(promoCodeRepository.findById(promo.getId())).willReturn(Optional.of(promo));

        service.delete(promo.getId());

        verify(promoCodeRepository).delete(promo);
    }

    @Test
    void unknownPromoCodeIsNotFound() {
        UUID id = UUID.randomUUID();
        given(promoCodeRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.promoCode(id)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsPromoCodes() {
        given(promoCodeRepository.findAllByOrderByCodeAsc()).willReturn(List.of(drop100()));

        assertThat(service.allPromoCodes()).extracting(PromoCodeResponse::code).containsExactly("DROP100");
    }
}
