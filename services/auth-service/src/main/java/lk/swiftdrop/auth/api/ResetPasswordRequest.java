package lk.swiftdrop.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The new password follows the signup rules. */
public record ResetPasswordRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(min = 12, max = 72) @ValidSignupPassword String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest[masked]";
    }
}
