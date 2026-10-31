package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.outbox.service.OutboxService;
import lk.swiftdrop.parcel.parcel.client.PricingQuote;
import lk.swiftdrop.parcel.parcel.client.PricingServiceClient;
import lk.swiftdrop.parcel.parcel.client.QuoteRequest;
import lk.swiftdrop.parcel.parcel.dto.CreateInternalParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.CreateInternalParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.CreateParcelRequest;
import lk.swiftdrop.parcel.parcel.dto.ParcelResponse;
import lk.swiftdrop.parcel.parcel.dto.RecipientContactsResponse;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelStatusHistory;
import lk.swiftdrop.parcel.parcel.repository.ParcelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ParcelService {

    private static final Logger log = LoggerFactory.getLogger(ParcelService.class);

    private final ParcelRepository parcelRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final PricingServiceClient pricingServiceClient;
    private final OutboxService outboxService;

    public ParcelService(ParcelRepository parcelRepository,
                         TrackingNumberGenerator trackingNumberGenerator,
                         PricingServiceClient pricingServiceClient,
                         OutboxService outboxService) {
        this.parcelRepository = parcelRepository;
        this.trackingNumberGenerator = trackingNumberGenerator;
        this.pricingServiceClient = pricingServiceClient;
        this.outboxService = outboxService;
    }

    @Transactional
    public ParcelResponse createCustomerParcel(UUID customerId, CreateParcelRequest request) {
        PricingQuote quote = pricingServiceClient.getQuote(new QuoteRequest(request.pickupZone(),
                request.dropoffZone(), request.weightKg(), request.size(), request.speed(), request.promoCode()));

        Parcel parcel = new Parcel(UUID.randomUUID(), trackingNumberGenerator.generateUniqueTrackingNumber(),
                customerId, null, request.pickupAddress(), request.dropoffAddress(), request.recipientName(),
                request.recipientPhone(), request.size(), request.weightKg(), quote.total(), quote.currency());
        parcel.setPickupZone(request.pickupZone());
        parcel.setDropoffZone(request.dropoffZone());
        parcel.setSpeed(request.speed());
        parcel.setPromoCode(request.promoCode());

        Parcel savedParcel = saveNewParcel(parcel, quote, customerId, "CUSTOMER", "Delivery booking created");
        log.info("Created customer parcel {} with tracking {}", savedParcel.getId(), savedParcel.getTrackingNo());
        return ParcelResponse.fromEntity(savedParcel);
    }

    /** The fee always comes from pricing-service's quote; a merchant cannot set its own price. */
    @Transactional
    public CreateInternalParcelResponse createMerchantParcel(CreateInternalParcelRequest request) {
        PricingQuote quote = pricingServiceClient.getQuote(new QuoteRequest(request.pickupZone(),
                request.dropoffZone(), request.weightKg(), request.size(), request.speed(), null));

        Parcel parcel = new Parcel(UUID.randomUUID(), trackingNumberGenerator.generateUniqueTrackingNumber(),
                null, request.merchantId(), request.pickupAddress(), request.dropoffAddress(),
                request.recipientName(), request.recipientPhone(), request.size(), request.weightKg(),
                quote.total(), quote.currency());
        parcel.setPickupZone(request.pickupZone());
        parcel.setDropoffZone(request.dropoffZone());
        parcel.setRecipientEmail(request.recipientEmail());
        parcel.setSpeed(request.speed());

        Parcel savedParcel = saveNewParcel(parcel, quote, request.merchantId(), "MERCHANT", "Merchant order created");
        log.info("Created merchant parcel {} for merchant {} with tracking {}",
                savedParcel.getId(), request.merchantId(), savedParcel.getTrackingNo());
        return CreateInternalParcelResponse.fromEntity(savedParcel);
    }

    /** The recipient's contact details for notification-service; personal data, so it is never logged. */
    @Transactional(readOnly = true)
    public RecipientContactsResponse getRecipientContacts(UUID parcelId) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new NotFoundException("Parcel not found: " + parcelId));
        return RecipientContactsResponse.fromEntity(parcel);
    }

    /** Stores the quote's breakdown, the CREATED history row and the outbox event in the caller's transaction. */
    private Parcel saveNewParcel(Parcel parcel, PricingQuote quote, UUID createdBy, String role, String note) {
        parcel.applyFee(quote.routeType(), quote.baseFee(), quote.weightFee(), quote.sizeFee(), quote.expressFee(),
                quote.discount(), quote.total(), quote.currency());
        parcel.addStatusHistory(new ParcelStatusHistory(UUID.randomUUID(), parcel, ParcelStatus.CREATED,
                createdBy, role, note, null));

        Parcel savedParcel = parcelRepository.save(parcel);
        outboxService.enqueueStatusChangedEvent(savedParcel, null, note);
        return savedParcel;
    }
}
