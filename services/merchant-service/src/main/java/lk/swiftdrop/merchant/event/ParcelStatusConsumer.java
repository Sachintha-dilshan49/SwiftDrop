package lk.swiftdrop.merchant.event;

import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.merchant.service.MerchantOrderStatusService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Keeps each merchant order's status in step with its parcel. Parcels without a merchant are ignored. */
@Component
public class ParcelStatusConsumer {

    private final MerchantOrderStatusService statusService;

    public ParcelStatusConsumer(MerchantOrderStatusService statusService) {
        this.statusService = statusService;
    }

    @RabbitListener(queues = "${rabbitmq.queues.merchant-orders}")
    public void onStatusChange(ParcelStatusChanged event) {
        if (event.merchantId() != null) {
            statusService.apply(event);
        }
    }
}
