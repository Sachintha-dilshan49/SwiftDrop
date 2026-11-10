package lk.swiftdrop.auth.api;

import java.util.UUID;
import lk.swiftdrop.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final AuthService authService;

    public InternalUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{userId}/contact")
    public UserContactResponse getContact(@PathVariable UUID userId) {
        return authService.getUserContact(userId);
    }
}
