package lk.swiftdrop.merchant.client;

import jakarta.validation.constraints.NotBlank;

public record GrantRoleRequest(@NotBlank String role) {
}

