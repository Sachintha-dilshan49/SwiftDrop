package lk.swiftdrop.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 255) String emailOrMobile,
        @NotBlank @Size(max = 72) String password) {
}
