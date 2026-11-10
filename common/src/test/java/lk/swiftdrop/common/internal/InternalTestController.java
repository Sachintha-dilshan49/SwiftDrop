package lk.swiftdrop.common.internal;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InternalTestController {

    @GetMapping("/internal/whoami")
    List<String> internalWhoAmI(Authentication authentication) {
        return authorities(authentication);
    }

    @GetMapping("/api/v1/things")
    List<String> jwtRoute(Authentication authentication) {
        return authorities(authentication);
    }

    private static List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
