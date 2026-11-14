package lk.swiftdrop.auth.api;

import jakarta.validation.constraints.NotBlank;

public record GrantRoleRequest(@NotBlank String role) {
}

