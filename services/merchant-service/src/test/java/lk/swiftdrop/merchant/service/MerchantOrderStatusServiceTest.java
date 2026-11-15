package lk.swiftdrop.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.merchant.entity.MerchantOrder;
import lk.swiftdrop.merchant.entity.ProcessedEvent;
import lk.swiftdrop.merchant.repository.MerchantOrderRepository;
import lk.swiftdrop.merchant.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MerchantOrderStatusServiceTest {

    private static final UUID MERCHANT = UUID.randomUUID();
    private static final UUID PARCEL = UUID.randomUUID();

    private final MerchantOrderRepository orders = mock(MerchantOrderRepository.class);
    private final ProcessedEventRepository processed = mock(ProcessedEventRepository.class);
    private final MerchantOrderStatusService service = new MerchantOrderStatusService(orders, processed);

    @Test
    void orderMovesForwardToTheParcelsStatus() {
        MerchantOrder order = orderWithStatus("PICKED_UP");

        service.apply(event(MERCHANT, ParcelStatus.OUT_FOR_DELIVERY));

        assertThat(order.getStatus()).isEqualTo("OUT_FOR_DELIVERY");
        verify(orders).save(order);
        verify(processed).save(any(ProcessedEvent.class));
    }

    @Test
    void failedDeliveryEndsTheOrder() {
        MerchantOrder order = orderWithStatus("OUT_FOR_DELIVERY");

        service.apply(event(MERCHANT, ParcelStatus.FAILED));

        assertThat(order.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void olderEventArrivingLateDoesNotMoveTheOrderBack() {
        MerchantOrder order = orderWithStatus("DELIVERED");

        service.apply(event(MERCHANT, ParcelStatus.IN_TRANSIT));

        assertThat(order.getStatus()).isEqualTo("DELIVERED");
        verify(orders, never()).save(any());
        verify(processed).save(any(ProcessedEvent.class));
    }

    @Test
    void eventForAnotherMerchantsParcelIsIgnored() {
        MerchantOrder order = orderWithStatus("CREATED");

        service.apply(event(UUID.randomUUID(), ParcelStatus.PICKED_UP));

        assertThat(order.getStatus()).isEqualTo("CREATED");
        verify(orders, never()).save(any());
    }

    @Test
    void parcelWithoutAnOrderHereIsMarkedProcessed() {
        when(orders.findByParcelId(PARCEL)).thenReturn(Optional.empty());
        ParcelStatusChanged event = event(MERCHANT, ParcelStatus.PICKED_UP);

        service.apply(event);

        ArgumentCaptor<ProcessedEvent> saved = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processed).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getValue().getConsumerName()).isEqualTo(MerchantOrderStatusService.CONSUMER_NAME);
    }

    @Test
    void redeliveredEventChangesNothing() {
        ParcelStatusChanged event = event(MERCHANT, ParcelStatus.PICKED_UP);
        when(processed.existsByEventIdAndConsumerName(event.eventId(), MerchantOrderStatusService.CONSUMER_NAME))
                .thenReturn(true);

        service.apply(event);

        verify(orders, never()).findByParcelId(any());
        verify(processed, never()).save(any());
    }

    private MerchantOrder orderWithStatus(String status) {
        MerchantOrder order = new MerchantOrder(UUID.randomUUID(), MERCHANT, PARCEL, "SD-7K3Q-9P2M", "Kamal Silva",
                "0777654321", "5 Station Road", status, new BigDecimal("450.00"), "LKR", null);
        when(orders.findByParcelId(PARCEL)).thenReturn(Optional.of(order));
        return order;
    }

    private static ParcelStatusChanged event(UUID merchantId, ParcelStatus to) {
        return ParcelStatusChanged.of(Instant.now(), PARCEL, "SD-7K3Q-9P2M", null, merchantId, UUID.randomUUID(),
                null, to, null, null);
    }
}
