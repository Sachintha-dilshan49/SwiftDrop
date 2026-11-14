package lk.swiftdrop.auth.api;

import java.util.UUID;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.auth.service.InternalUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final AuthService authService;
    private final InternalUserService internalUserService;

    public InternalUserController(AuthService authService, InternalUserService internalUserService) {
        this.authService = authService;
        this.internalUserService = internalUserService;
    }

    @GetMapping("/{userId}/contact")
    public UserContactResponse getContact(@PathVariable UUID userId) {
        return authService.getUserContact(userId);
    }

    @PutMapping("/{userId}/roles/courier")
    public ResponseEntity<Void> grantCourierRole(@PathVariable UUID userId) {
        internalUserService.grantCourierRole(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/roles/merchant")
    public ResponseEntity<Void> grantMerchantRole(@PathVariable UUID userId) {
        internalUserService.grantMerchantRole(userId);
        return ResponseEntity.noContent().build();
    }
}
