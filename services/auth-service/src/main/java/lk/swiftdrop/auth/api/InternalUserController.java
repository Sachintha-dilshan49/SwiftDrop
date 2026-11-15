package lk.swiftdrop.auth.api;

import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lk.swiftdrop.auth.account.RoleName;
import lk.swiftdrop.auth.service.AuthService;
import lk.swiftdrop.auth.service.InternalUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** Used by merchant-service when an admin approves a merchant. */
    @PostMapping("/{userId}/roles")
    public ResponseEntity<Void> grantRole(
            @PathVariable UUID userId,
            @Valid @RequestBody GrantRoleRequest request) {
        authService.grantRole(userId, RoleName.valueOf(request.role().trim().toUpperCase(Locale.ROOT)));
        return ResponseEntity.noContent().build();
    }
}
