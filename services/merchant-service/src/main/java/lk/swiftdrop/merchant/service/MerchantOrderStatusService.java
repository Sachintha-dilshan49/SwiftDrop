package lk.swiftdrop.merchant.service;

import java.util.Map;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.merchant.entity.MerchantOrder;
import lk.swiftdrop.merchant.entity.ProcessedEvent;
import lk.swiftdrop.merchant.repository.MerchantOrderRepository;
import lk.swiftdrop.merchant.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies ParcelStatusChanged events to merchant orders. Only parcel-service decides a status (AGENTS.md rule 5);
 * this copy is what the merchant dashboard and the merchant API show.
 */
@Service
public class MerchantOrderStatusService {

    static final String CONSUMER_NAME = "merchant-orders";

    private static final Logger log = LoggerFactory.getLogger(MerchantOrderStatusService.class);

    /** Position in the delivery flow; DELIVERED and FAILED both end it. */
    private static final Map<String, Integer> STEP = Map.of(
            ParcelStatus.CREATED.name(), 0,
            ParcelStatus.PICKED_UP.name(), 1,
            ParcelStatus.IN_TRANSIT.name(), 2,
            ParcelStatus.OUT_FOR_DELIVERY.name(), 3,
            ParcelStatus.DELIVERED.name(), 4,
            ParcelStatus.FAILED.name(), 4);

    private final MerchantOrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    public MerchantOrderStatusService(MerchantOrderRepository orderRepository,
                                      ProcessedEventRepository processedEventRepository) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Moves the parcel's order forward to the event's status. A redelivered event, an event older than the order's
     * status, or a parcel with no order here (its CREATED event can arrive before the order is saved) changes nothing.
     */
    @Transactional
    public void apply(ParcelStatusChanged event) {
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId(), CONSUMER_NAME)) {
            return;
        }
        orderRepository.findByParcelId(event.parcelId())
                .filter(order -> order.getMerchantId().equals(event.merchantId()))
                .filter(order -> isLaterStep(event.toStatus().name(), order.getStatus()))
                .ifPresent(order -> moveTo(order, event));
        processedEventRepository.save(new ProcessedEvent(event.eventId(), CONSUMER_NAME));
    }

    private void moveTo(MerchantOrder order, ParcelStatusChanged event) {
        log.info("Order {} of parcel {}: {} -> {}", order.getId(), event.trackingNo(), order.getStatus(),
                event.toStatus());
        order.setStatus(event.toStatus().name());
        orderRepository.save(order);
    }

    private static boolean isLaterStep(String next, String current) {
        Integer currentStep = STEP.get(current);
        return currentStep == null || STEP.get(next) > currentStep;
    }
}
