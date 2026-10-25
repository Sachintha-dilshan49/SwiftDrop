package lk.swiftdrop.common.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecuredTestController {

    @GetMapping("/test/me")
    String me() {
        return "ok";
    }

    @GetMapping("/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    String admin() {
        return "admin";
    }

    @GetMapping("/test/public/info")
    String publicInfo() {
        return "public";
    }

    @GetMapping("/test/public/admin")
    @PreAuthorize("hasRole('ADMIN')")
    String publicButAdminOnly() {
        return "admin";
    }
}
