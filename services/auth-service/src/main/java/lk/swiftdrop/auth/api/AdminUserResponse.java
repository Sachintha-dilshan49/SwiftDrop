package lk.swiftdrop.auth.api;

import java.util.List;
import java.util.UUID;
import lk.swiftdrop.auth.account.AccountStatus;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String email,
        List<String> roles,
        AccountStatus accountStatus) {
}
