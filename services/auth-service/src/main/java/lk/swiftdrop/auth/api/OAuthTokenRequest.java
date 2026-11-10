package lk.swiftdrop.auth.api;

import jakarta.validation.constraints.NotBlank;

public record OAuthTokenRequest(@NotBlank String code) {
}
