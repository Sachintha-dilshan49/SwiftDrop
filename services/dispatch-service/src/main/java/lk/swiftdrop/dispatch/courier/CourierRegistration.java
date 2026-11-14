package lk.swiftdrop.dispatch.courier;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the dispatch record of a courier the first time they open their screens. auth-service decides who is a
 * courier (the COURIER role in their JWT); this service only needs a row to attach assignments to.
 */
@Component
public class CourierRegistration {

    private static final Logger log = LoggerFactory.getLogger(CourierRegistration.class);

    private final CourierRepository courierRepository;

    public CourierRegistration(CourierRepository courierRepository) {
        this.courierRepository = courierRepository;
    }

    /**
     * Its own transaction, so it also works from read-only ones. Two first requests at once: the second fails on the
     * unique user_id with a DataIntegrityViolationException, and the caller reads the row the first one created.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Courier register(UUID userId) {
        Courier courier = new Courier();
        courier.setUserId(userId);
        courier.setStatus(CourierStatus.AVAILABLE);
        Courier saved = courierRepository.saveAndFlush(courier);
        log.info("Registered courier {} for a new courier account", saved.getId());
        return saved;
    }
}
