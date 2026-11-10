package lk.swiftdrop.dispatch.unassigned;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dispatch/unassigned")
public class UnassignedParcelController {

    private final UnassignedParcelService unassignedParcelService;

    public UnassignedParcelController(UnassignedParcelService unassignedParcelService) {
        this.unassignedParcelService = unassignedParcelService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'ADMIN')")
    public List<UnassignedParcelResponse> getUnassignedParcels(@AuthenticationPrincipal Jwt jwt) {
        return unassignedParcelService.getUnassignedParcels(jwt.getTokenValue());
    }
}
