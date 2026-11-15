package lk.swiftdrop.auth.reset;

/** Published inside the reset transaction; the email goes out only after it commits. */
public record PasswordResetRequested(String recipient, String firstName, String resetLink) {

    @Override
    public String toString() {
        return "PasswordResetRequested[masked]";
    }
}
