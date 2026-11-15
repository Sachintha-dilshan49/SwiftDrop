package lk.swiftdrop.auth.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lk.swiftdrop.auth.account.AccountStatus;
import lk.swiftdrop.auth.account.RoleName;

public record AdminUserUpdateRequest(
        @NotEmpty Set<@NotNull RoleName> roles,
        @NotNull AccountStatus accountStatus) {
}
