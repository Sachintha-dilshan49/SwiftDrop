package lk.swiftdrop.parcel.parcel.service;

import lk.swiftdrop.common.error.ConflictException;
import lk.swiftdrop.common.event.ParcelStatus;
import lk.swiftdrop.parcel.parcel.dto.UpdateParcelStatusRequest;
import lk.swiftdrop.parcel.parcel.entity.Parcel;
import lk.swiftdrop.parcel.parcel.entity.ParcelSize;
import lk.swiftdrop.parcel.parcel.entity.ParcelStatusHistory;
import lk.swiftdrop.parcel.parcel.repository.ParcelRepository;
import lk.swiftdrop.parcel.parcel.repository.ParcelStatusHistoryRepository;
import lk.swiftdrop.parcel.support.AbstractContainersIT;
import lk.swiftdrop.parcel.support.ParcelFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Concurrent status changes to one parcel run one after another, so each sees the previous result
 * and the outbox events are ordered like the commits.
 */
class ParcelStatusConcurrencyIT extends AbstractContainersIT {

    @Autowired
    private ParcelService parcelService;

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private ParcelStatusHistoryRepository historyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private UUID parcelId;

    @BeforeEach
    void createParcel() {
        parcelId = UUID.randomUUID();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Parcel parcel = new Parcel(parcelId, "SD-" + randomBlock() + "-" + randomBlock(), UUID.randomUUID(),
                    null, "Pickup", "Dropoff", "Recipient", "0771234567", ParcelSize.SMALL, BigDecimal.ONE,
                    new BigDecimal("350.00"), "LKR");
            ParcelFixtures.withRouteAndFee(parcel);
            parcel.addStatusHistory(new ParcelStatusHistory(UUID.randomUUID(), parcel, ParcelStatus.CREATED,
                    parcel.getCustomerId(), "CUSTOMER", "Created", null));
            parcelRepository.save(parcel);
        });
    }

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("A second change waits for the first, then builds on its result; outbox order matches commit order")
    void secondChangeWaitsAndSeesFirstResult() throws Exception {
        CountDownLatch firstChanged = new CountDownLatch(1);
        CountDownLatch commitFirst = new CountDownLatch(1);

        Future<?> first = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            parcelService.updateParcelStatus(parcelId, request(ParcelStatus.PICKED_UP));
            firstChanged.countDown();
            await(commitFirst);
        }));
        assertThat(firstChanged.await(10, TimeUnit.SECONDS)).isTrue();

        // Without the row lock this would read CREATED and fail as CREATED -> IN_TRANSIT
        Future<?> second = executor.submit(() -> parcelService.updateParcelStatus(parcelId, request(ParcelStatus.IN_TRANSIT)));
        assertThatThrownBy(() -> second.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

        commitFirst.countDown();
        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);

        assertThat(parcelRepository.findById(parcelId).orElseThrow().getStatus()).isEqualTo(ParcelStatus.IN_TRANSIT);
        assertThat(historyRepository.findByParcelIdOrderByCreatedAtAsc(parcelId))
                .extracting("status")
                .containsExactly(ParcelStatus.CREATED, ParcelStatus.PICKED_UP, ParcelStatus.IN_TRANSIT);
        assertThat(jdbcTemplate.queryForList(
                "SELECT routing_key FROM outbox_events WHERE aggregate_id = ? ORDER BY seq", String.class, parcelId))
                .containsExactly("parcel.status.picked_up", "parcel.status.in_transit");
    }

    @Test
    @DisplayName("The same change requested twice at once is applied once; the second gets 409")
    void duplicateConcurrentChangeIsRejected() throws Exception {
        CountDownLatch firstChanged = new CountDownLatch(1);
        CountDownLatch commitFirst = new CountDownLatch(1);

        Future<?> first = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            parcelService.updateParcelStatus(parcelId, request(ParcelStatus.PICKED_UP));
            firstChanged.countDown();
            await(commitFirst);
        }));
        assertThat(firstChanged.await(10, TimeUnit.SECONDS)).isTrue();

        Future<?> second = executor.submit(() -> parcelService.updateParcelStatus(parcelId, request(ParcelStatus.PICKED_UP)));
        commitFirst.countDown();
        first.get(10, TimeUnit.SECONDS);

        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ConflictException.class);
        assertThat(historyRepository.findByParcelIdOrderByCreatedAtAsc(parcelId))
                .extracting("status")
                .containsExactly(ParcelStatus.CREATED, ParcelStatus.PICKED_UP);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id = ?", Integer.class, parcelId)).isEqualTo(1);
    }

    private static UpdateParcelStatusRequest request(ParcelStatus status) {
        return new UpdateParcelStatusRequest(status, null, null, null, null, UUID.randomUUID(), "DISPATCHER");
    }

    private static String randomBlock() {
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder block = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            block.append(alphabet.charAt((int) (Math.random() * alphabet.length())));
        }
        return block.toString();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
