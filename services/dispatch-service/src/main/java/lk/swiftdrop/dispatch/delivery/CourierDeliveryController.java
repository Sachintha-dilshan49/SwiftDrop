package lk.swiftdrop.dispatch.delivery;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courier/deliveries")
@PreAuthorize("hasRole('COURIER')")
public class CourierDeliveryController {

    private final CourierDeliveryService courierDeliveryService;

    public CourierDeliveryController(CourierDeliveryService courierDeliveryService) {
        this.courierDeliveryService = courierDeliveryService;
    }

    @GetMapping("/today")
    public List<CourierDeliveryResponse> getTodaysDeliveries(@AuthenticationPrincipal Jwt jwt) {
        return courierDeliveryService.getTodaysDeliveries(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{assignmentId}")
    public CourierDeliveryResponse getDelivery(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID assignmentId) {
        return courierDeliveryService.getDelivery(UUID.fromString(jwt.getSubject()), assignmentId);
    }
}
