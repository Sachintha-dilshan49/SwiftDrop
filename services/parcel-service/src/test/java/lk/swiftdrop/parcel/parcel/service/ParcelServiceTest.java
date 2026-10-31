package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.error.NotFoundException;
import lk.swiftdrop.common.error.ServiceUnavailableException;
import lk.swiftdrop.common.error.ValidationException;
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
import lk.swiftdrop.parcel.parcel.dto.UpdateParcelStatusRequest;
import lk.swiftdrop.parcel.parcel.entity.DeliverySpeed;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.entity.RouteType;
import lk.swiftdrop.parcel.parcel.repository.ParcelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelServiceTest {

    @Mock
    private ParcelRepository parcelRepository;
    @Mock
    private TrackingNumberGenerator trackingNumberGenerator;
    @Mock
    private PricingServiceClient pricingServiceClient;
    @Mock
    private ParcelStateMachine parcelStateMachine;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ParcelService parcelService;

    private UUID customerId;
    private Parcel sampleParcel;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        sampleParcel = new Parcel(
                UUID.randomUUID(),
                "SD-7K3Q-9P2M",
                customerId,
                null,
                "123 Main St, Colombo",
                "456 High St, Kandy",
                "Nimal Perera",
                "+94771234567",
                ParcelSize.MEDIUM,
                new BigDecimal("2.50"),
                new BigDecimal("650.00"),
                "LKR"
        );
        sampleParcel.setStatus(ParcelStatus.CREATED);
    }

    /** Colombo South to inter-city, 2.5 kg, MEDIUM, with DROP100: 650 + 80 + 100 + 0 - 100 = 730. */
    private static final PricingQuote QUOTE = new PricingQuote(RouteType.INTER_CITY, new BigDecimal("650.00"),
            new BigDecimal("80.00"), new BigDecimal("100.00"), new BigDecimal("0.00"), new BigDecimal("100.00"),
            new BigDecimal("730.00"), "LKR");

    private static CreateParcelRequest customerRequest() {
        return new CreateParcelRequest("123 Main St, Colombo", "COLOMBO_SOUTH", "456 High St, Kandy", "INTER_CITY",
                "Nimal Perera", "+94771234567", ParcelSize.MEDIUM, new BigDecimal("2.50"), DeliverySpeed.STANDARD,
                "DROP100");
    }

    private static CreateInternalParcelRequest merchantRequest(UUID merchantId) {
        return new CreateInternalParcelRequest(merchantId, "Warehouse A, Colombo", "COLOMBO_SOUTH",
                "Customer Home, Galle", "INTER_CITY", "Sunil Fernando", "+94712345678", "sunil@example.com",
                ParcelSize.MEDIUM, new BigDecimal("2.50"), DeliverySpeed.EXPRESS);
    }

    private Parcel savedParcel() {
        ArgumentCaptor<Parcel> saved = ArgumentCaptor.forClass(Parcel.class);
        verify(parcelRepository).save(saved.capture());
        return saved.getValue();
    }

    private static void assertStoresQuote(Parcel parcel) {
        assertThat(parcel.getRouteType()).isEqualTo(RouteType.INTER_CITY);
        assertThat(parcel.getBaseFee()).isEqualByComparingTo("650.00");
        assertThat(parcel.getWeightFee()).isEqualByComparingTo("80.00");
        assertThat(parcel.getSizeFee()).isEqualByComparingTo("100.00");
        assertThat(parcel.getExpressFee()).isEqualByComparingTo("0.00");
        assertThat(parcel.getDiscount()).isEqualByComparingTo("100.00");
        assertThat(parcel.getFeeAmount()).isEqualByComparingTo("730.00");
        assertThat(parcel.getFeeCurrency()).isEqualTo("LKR");
        assertThat(parcel.getStatus()).isEqualTo(ParcelStatus.CREATED);
    }

    @Test
    @DisplayName("createCustomerParcel stores the route, the quote's breakdown, the first history row and an outbox event")
    void createCustomerParcelStoresQuoteHistoryAndEvent() {
        when(pricingServiceClient.getQuote(new QuoteRequest("COLOMBO_SOUTH", "INTER_CITY", new BigDecimal("2.50"),
                ParcelSize.MEDIUM, DeliverySpeed.STANDARD, "DROP100"))).thenReturn(QUOTE);
        when(trackingNumberGenerator.generateUniqueTrackingNumber()).thenReturn("SD-7K3Q-9P2M");
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParcelResponse response = parcelService.createCustomerParcel(customerId, customerRequest());

        assertThat(response.trackingNo()).isEqualTo("SD-7K3Q-9P2M");
        assertThat(response.feeAmount()).isEqualByComparingTo("730.00");
        assertThat(response.status()).isEqualTo(ParcelStatus.CREATED);

        Parcel parcel = savedParcel();
        assertStoresQuote(parcel);
        assertThat(parcel.getCustomerId()).isEqualTo(customerId);
        assertThat(parcel.getMerchantId()).isNull();
        assertThat(parcel.getPickupZone()).isEqualTo("COLOMBO_SOUTH");
        assertThat(parcel.getDropoffZone()).isEqualTo("INTER_CITY");
        assertThat(parcel.getSpeed()).isEqualTo(DeliverySpeed.STANDARD);
        assertThat(parcel.getPromoCode()).isEqualTo("DROP100");
        assertThat(parcel.getRecipientEmail()).isNull();
        assertThat(parcel.getStatusHistory()).singleElement().satisfies(history -> {
            assertThat(history.getStatus()).isEqualTo(ParcelStatus.CREATED);
            assertThat(history.getChangedBy()).isEqualTo(customerId);
            assertThat(history.getChangedByRole()).isEqualTo("CUSTOMER");
        });
        verify(outboxService).enqueueStatusChangedEvent(parcel, null, "Delivery booking created");
    }

    @Test
    @DisplayName("createCustomerParcel saves nothing and enqueues no event when pricing is unavailable")
    void createCustomerParcelSavesNothingWhenPricingUnavailable() {
        when(pricingServiceClient.getQuote(any()))
                .thenThrow(new ServiceUnavailableException("pricing-service is not available right now."));

        assertThatThrownBy(() -> parcelService.createCustomerParcel(customerId, customerRequest()))
                .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(parcelRepository, trackingNumberGenerator, outboxService);
    }

    @Test
    @DisplayName("createCustomerParcel saves nothing when pricing rejects the input")
    void createCustomerParcelSavesNothingWhenPricingRejectsInput() {
        when(pricingServiceClient.getQuote(any()))
                .thenThrow(new ValidationException("Promo code DROP100 has expired"));

        assertThatThrownBy(() -> parcelService.createCustomerParcel(customerId, customerRequest()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(parcelRepository, trackingNumberGenerator, outboxService);
    }

    @Test
    @DisplayName("createMerchantParcel takes the fee from pricing's quote and stores the recipient's email")
    void createMerchantParcelTakesFeeFromQuote() {
        UUID merchantId = UUID.randomUUID();
        when(pricingServiceClient.getQuote(new QuoteRequest("COLOMBO_SOUTH", "INTER_CITY", new BigDecimal("2.50"),
                ParcelSize.MEDIUM, DeliverySpeed.EXPRESS, null))).thenReturn(QUOTE);
        when(trackingNumberGenerator.generateUniqueTrackingNumber()).thenReturn("SD-9B2N-4X1Z");
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateInternalParcelResponse response = parcelService.createMerchantParcel(merchantRequest(merchantId));

        assertThat(response.trackingNo()).isEqualTo("SD-9B2N-4X1Z");
        assertThat(response.status()).isEqualTo(ParcelStatus.CREATED);
        assertThat(response.feeAmount()).isEqualByComparingTo("730.00");
        assertThat(response.feeCurrency()).isEqualTo("LKR");

        Parcel parcel = savedParcel();
        assertThat(response.id()).isEqualTo(parcel.getId());
        assertStoresQuote(parcel);
        assertThat(parcel.getMerchantId()).isEqualTo(merchantId);
        assertThat(parcel.getCustomerId()).isNull();
        assertThat(parcel.getRecipientEmail()).isEqualTo("sunil@example.com");
        assertThat(parcel.getSpeed()).isEqualTo(DeliverySpeed.EXPRESS);
        assertThat(parcel.getPromoCode()).isNull();
        assertThat(parcel.getStatusHistory()).singleElement().satisfies(history -> {
            assertThat(history.getChangedBy()).isEqualTo(merchantId);
            assertThat(history.getChangedByRole()).isEqualTo("MERCHANT");
        });
        verify(outboxService).enqueueStatusChangedEvent(parcel, null, "Merchant order created");
    }

    @Test
    @DisplayName("createMerchantParcel saves nothing when pricing is unavailable")
    void createMerchantParcelSavesNothingWhenPricingUnavailable() {
        when(pricingServiceClient.getQuote(any()))
                .thenThrow(new ServiceUnavailableException("pricing-service is not available right now."));

        assertThatThrownBy(() -> parcelService.createMerchantParcel(merchantRequest(UUID.randomUUID())))
                .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(parcelRepository, trackingNumberGenerator, outboxService);
    }

    @Test
    @DisplayName("getRecipientContacts returns the recipient's name, phone and email")
    void getRecipientContactsReturnsRecipientDetails() {
        UUID parcelId = UUID.randomUUID();
        Parcel parcel = new Parcel(parcelId, "SD-9B2N-4X1Z", null, UUID.randomUUID(), "Warehouse A, Colombo",
                "Customer Home, Galle", "Sunil Fernando", "+94712345678", ParcelSize.SMALL, BigDecimal.ONE,
                new BigDecimal("350.00"), "LKR");
        parcel.setRecipientEmail("sunil@example.com");
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));

        assertThat(parcelService.getRecipientContacts(parcelId))
                .isEqualTo(new RecipientContactsResponse("Sunil Fernando", "+94712345678", "sunil@example.com"));
    }

    @Test
    @DisplayName("getRecipientContacts of an unknown parcel is a 404")
    void getRecipientContactsOfUnknownParcelIsNotFound() {
        UUID parcelId = UUID.randomUUID();
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.getRecipientContacts(parcelId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateParcelStatus validates state machine, updates status, records history and enqueues event")
    void updateParcelStatusSuccess() {
        UUID parcelId = sampleParcel.getId();
        UUID courierId = UUID.randomUUID();
        UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                ParcelStatus.PICKED_UP,
                courierId,
                "Picked up from sender",
                null,
                null,
                courierId,
                "ROLE_COURIER"
        );

        when(parcelRepository.findByIdForUpdate(parcelId)).thenReturn(Optional.of(sampleParcel));
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParcelResponse response = parcelService.updateParcelStatus(parcelId, request);

        assertThat(response.status()).isEqualTo(ParcelStatus.PICKED_UP);
        assertThat(response.courierId()).isEqualTo(courierId);
        verify(parcelRepository, never()).findById(any());
        verify(parcelStateMachine).validateTransition(sampleParcel, ParcelStatus.PICKED_UP, null);
        verify(parcelStateMachine).validateAuthorization(sampleParcel, courierId, "ROLE_COURIER");
        verify(outboxService).enqueueStatusChangedEvent(sampleParcel, ParcelStatus.CREATED, "Picked up from sender");
    }

    @Test
    @DisplayName("updateParcelStatus of an unknown parcel is a 404 and changes nothing")
    void updateParcelStatusOfUnknownParcelIsNotFound() {
        UUID parcelId = UUID.randomUUID();
        when(parcelRepository.findByIdForUpdate(parcelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.updateParcelStatus(parcelId, dispatcherUpdate(ParcelStatus.PICKED_UP, null)))
                .isInstanceOf(NotFoundException.class);

        verify(parcelRepository, never()).save(any());
        verifyNoInteractions(parcelStateMachine, outboxService);
    }

    @Test
    @DisplayName("A rejected status change leaves the parcel as it was and enqueues no event")
    void rejectedStatusChangeChangesNothing() {
        when(parcelRepository.findByIdForUpdate(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));
        doThrow(new ConflictException("invalid-status-transition", "Invalid status transition",
                "Parcel cannot move from CREATED to DELIVERED."))
                .when(parcelStateMachine).validateTransition(sampleParcel, ParcelStatus.DELIVERED, null);

        assertThatThrownBy(() -> parcelService.updateParcelStatus(sampleParcel.getId(),
                dispatcherUpdate(ParcelStatus.DELIVERED, null)))
                .isInstanceOf(ConflictException.class);

        assertThat(sampleParcel.getStatus()).isEqualTo(ParcelStatus.CREATED);
        assertThat(sampleParcel.getStatusHistory()).isEmpty();
        verify(parcelRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("updateParcelStatus to FAILED stores the failure reason, a history row and the event")
    void failedStatusStoresReasonHistoryAndEvent() {
        sampleParcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        when(parcelRepository.findByIdForUpdate(sampleParcel.getId())).thenReturn(Optional.of(sampleParcel));
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParcelResponse response = parcelService.updateParcelStatus(sampleParcel.getId(),
                dispatcherUpdate(ParcelStatus.FAILED, "Recipient not available"));

        assertThat(response.status()).isEqualTo(ParcelStatus.FAILED);
        assertThat(response.failureReason()).isEqualTo("Recipient not available");
        assertThat(sampleParcel.getStatusHistory()).singleElement().satisfies(history -> {
            assertThat(history.getStatus()).isEqualTo(ParcelStatus.FAILED);
            assertThat(history.getChangedByRole()).isEqualTo("DISPATCHER");
        });
        verify(outboxService).enqueueStatusChangedEvent(sampleParcel, ParcelStatus.OUT_FOR_DELIVERY, "Dispatcher update");
    }

    private static UpdateParcelStatusRequest dispatcherUpdate(ParcelStatus status, String failureReason) {
        return new UpdateParcelStatusRequest(status, null, "Dispatcher update", null, failureReason,
                UUID.randomUUID(), "DISPATCHER");
    }
}
