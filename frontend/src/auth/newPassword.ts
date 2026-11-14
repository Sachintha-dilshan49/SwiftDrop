const MIN_PASSWORD_LENGTH = 12;
const MAX_PASSWORD_BYTES = 72; // bcrypt limit, same rule as auth-service

/** Mirrors auth-service: 12 characters or more, at most 72 bytes, both fields equal. */
export function newPasswordError(password: string, confirmation: string): string | null {
  if (password.length < MIN_PASSWORD_LENGTH) {
    return `Use at least ${MIN_PASSWORD_LENGTH} characters.`;
  }
  if (new TextEncoder().encode(password).length > MAX_PASSWORD_BYTES) {
    return "This password is too long. Use a shorter one.";
  }
  if (password !== confirmation) {
    return "The two passwords do not match.";
  }
  return null;
}
