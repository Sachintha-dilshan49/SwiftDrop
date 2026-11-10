package lk.swiftdrop.pricing.quote.service;

import lk.swiftdrop.common.error.ValidationException;
import lk.swiftdrop.pricing.promo.repository.PromoCodeRepository;
import lk.swiftdrop.pricing.quote.dto.PricingQuoteResponse;
import lk.swiftdrop.pricing.quote.dto.QuoteRequest;
import lk.swiftdrop.pricing.quote.fee.ExtraWeightFee;
import lk.swiftdrop.pricing.quote.fee.FeeCalculator;
import lk.swiftdrop.pricing.quote.fee.PromoDiscount;
import lk.swiftdrop.pricing.quote.fee.RouteBaseFee;
import lk.swiftdrop.pricing.quote.fee.SizeSurcharge;
import lk.swiftdrop.pricing.quote.fee.SpeedSurcharge;
import lk.swiftdrop.pricing.quote.model.DeliverySpeed;
import lk.swiftdrop.pricing.quote.model.ParcelSize;
import lk.swiftdrop.pricing.rule.repository.PricingRuleRepository;
import lk.swiftdrop.pricing.zone.entity.Zone;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;
import lk.swiftdrop.pricing.zone.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static lk.swiftdrop.pricing.support.PricingFixtures.NOW;
import static lk.swiftdrop.pricing.support.PricingFixtures.drop100;
import static lk.swiftdrop.pricing.support.PricingFixtures.seedRules;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleQuote;
import static lk.swiftdrop.pricing.support.QuoteSamples.workedExampleRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    private QuoteService service;

    @BeforeEach
    void setUp() {
        FeeCalculator calculator = new FeeCalculator(List.of(new RouteBaseFee(), new ExtraWeightFee(),
                new SizeSurcharge(), new SpeedSurcharge(), new PromoDiscount()));
        service = new QuoteService(zoneRepository, pricingRuleRepository, promoCodeRepository, calculator,
                Clock.fixed(NOW, ZoneOffset.UTC));

        given(zoneRepository.findByCode("COLOMBO_SOUTH"))
                .willReturn(Optional.of(zone("COLOMBO_SOUTH", ZoneGroup.COLOMBO, true)));
        given(zoneRepository.findByCode("COLOMBO_CENTRAL"))
                .willReturn(Optional.of(zone("COLOMBO_CENTRAL", ZoneGroup.COLOMBO, true)));
        given(pricingRuleRepository.findByActiveTrue()).willReturn(seedRules());
        given(promoCodeRepository.findByCode("DROP100")).willReturn(Optional.of(drop100()));
    }

    @Test
    void quotesTheWorkedExampleWithDrop100At390() {
        assertThat(service.quote(workedExampleRequest())).isEqualTo(workedExampleQuote());
    }

    @Test
    @DisplayName("Promo codes are matched in upper case, so drop100 works like DROP100")
    void promoCodeIsMatchedInUpperCase() {
        PricingQuoteResponse quote = service.quote(request("COLOMBO_SOUTH", "COLOMBO_CENTRAL", " drop100 "));

        assertThat(quote.total()).isEqualTo(new BigDecimal("390.00"));
        verify(promoCodeRepository).findByCode("DROP100");
    }

    @Test
    void blankPromoCodeMeansNoPromo() {
        PricingQuoteResponse quote = service.quote(request("COLOMBO_SOUTH", "COLOMBO_CENTRAL", ""));

        assertThat(quote.total()).isEqualTo(new BigDecimal("490.00"));
        verify(promoCodeRepository, never()).findByCode(anyString());
    }

    @Test
    void unknownPickupZoneIsRejected() {
        given(zoneRepository.findByCode("JAFFNA")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.quote(request("JAFFNA", "COLOMBO_CENTRAL", null)))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                        .extracting("field").containsExactly("pickupZone"))
                .hasMessage("Pickup zone JAFFNA is not served.");
    }

    @Test
    void inactiveDropoffZoneIsRejected() {
        given(zoneRepository.findByCode("GALLE")).willReturn(Optional.of(zone("GALLE", ZoneGroup.INTER_CITY, false)));

        assertThatThrownBy(() -> service.quote(request("COLOMBO_SOUTH", "GALLE", null)))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getErrors())
                        .extracting("field").containsExactly("dropoffZone"))
                .hasMessage("Drop-off zone GALLE is not served.");
    }

    @Test
    void unknownPromoCodeIsRejected() {
        given(promoCodeRepository.findByCode("NOPE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.quote(request("COLOMBO_SOUTH", "COLOMBO_CENTRAL", "nope")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Promo code NOPE is not valid.");
    }

    private static QuoteRequest request(String pickup, String dropoff, String promoCode) {
        return new QuoteRequest(pickup, dropoff, new BigDecimal("1.2"), ParcelSize.SMALL, DeliverySpeed.EXPRESS,
                promoCode);
    }

    private static Zone zone(String code, ZoneGroup group, boolean active) {
        return new Zone(code, code, group, active, List.of());
    }
}
