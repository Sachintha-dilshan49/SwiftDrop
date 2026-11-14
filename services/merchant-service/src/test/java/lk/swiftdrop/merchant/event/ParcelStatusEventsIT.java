package lk.swiftdrop.merchant.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lk.swiftdrop.common.event.ParcelEvents;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.common.event.ParcelStatusChanged;
import lk.swiftdrop.merchant.MerchantIntegrationTest;
import lk.swiftdrop.merchant.entity.Merchant;
import lk.swiftdrop.merchant.entity.MerchantOrder;
import lk.swiftdrop.merchant.entity.MerchantStatus;
import lk.swiftdrop.merchant.repository.MerchantOrderRepository;
import lk.swiftdrop.merchant.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Events published the way parcel-service publishes them reach the merchant's order through RabbitMQ. */
class ParcelStatusEventsIT extends MerchantIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private MerchantOrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Merchant merchant;
    private MerchantOrder order;

    @BeforeEach
    void merchantWithOneOrder() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        merchant = merchantRepository.save(new Merchant(UUID.randomUUID(), UUID.randomUUID(), "Shop " + unique,
                "PV" + unique, "Nimal Perera", unique + "@shop.example.com", "0771234567", "1 Galle Road",
                MerchantStatus.APPROVED));
        order = orderRepository.save(new MerchantOrder(UUID.randomUUID(), merchant.getId(), UUID.randomUUID(),
                "SD-7K3Q-9P2M", "Kamal Silva", "0777654321", "5 Station Road", "CREATED",
                new BigDecimal("450.00"), "LKR", null));
    }

    @Test
    void orderFollowsItsParcelThroughTheDeliveryFlow() {
        publish(event(ParcelStatus.CREATED, ParcelStatus.PICKED_UP));
        publish(event(ParcelStatus.PICKED_UP, ParcelStatus.IN_TRANSIT));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("IN_TRANSIT"));
    }

    @Test
    void redeliveredEventIsAppliedOnce() {
        ParcelStatusChanged pickedUp = event(ParcelStatus.CREATED, ParcelStatus.PICKED_UP);
        publish(pickedUp);
        publish(pickedUp);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from processed_events where event_id = ?", Integer.class,
                        pickedUp.eventId())).isEqualTo(1));
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo("PICKED_UP");
    }

    private ParcelStatusChanged event(ParcelStatus from, ParcelStatus to) {
        return ParcelStatusChanged.of(Instant.now(), order.getParcelId(), order.getTrackingNo(), null,
                merchant.getId(), UUID.randomUUID(), from, to, null, null);
    }

    private void publish(ParcelStatusChanged event) {
        rabbitTemplate.convertAndSend(ParcelEvents.EXCHANGE,
                ParcelEvents.ROUTING_KEY_PREFIX + event.toStatus().name().toLowerCase(), event);
    }
}
