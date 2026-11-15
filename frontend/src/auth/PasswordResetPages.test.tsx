import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ForgotPasswordPage, ResetPasswordPage } from "./PasswordResetPages";
import { newPasswordError } from "./newPassword";

const { requestPasswordResetMock, resetPasswordMock } = vi.hoisted(() => ({
  requestPasswordResetMock: vi.fn(),
  resetPasswordMock: vi.fn(),
}));

vi.mock("./authApi", () => ({
  requestPasswordReset: (...args: unknown[]) => requestPasswordResetMock(...args),
  resetPassword: (...args: unknown[]) => resetPasswordMock(...args),
}));

describe("newPasswordError", () => {
  it("mirrors the signup password rules", () => {
    expect(newPasswordError("short", "short")).toMatch(/at least 12/);
    expect(newPasswordError("é".repeat(37), "é".repeat(37))).toMatch(/too long/);
    expect(newPasswordError("long-enough-password", "different-password")).toMatch(/do not match/);
    expect(newPasswordError("long-enough-password", "long-enough-password")).toBeNull();
  });
});

describe("ForgotPasswordPage", () => {
  beforeEach(() => {
    requestPasswordResetMock.mockReset();
    window.history.replaceState({}, "", "/forgot-password");
  });

  it("sends a reset link and says the same thing whether or not the account exists", async () => {
    requestPasswordResetMock.mockResolvedValue(undefined);
    render(<ForgotPasswordPage />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: " demo@example.test " } });
    fireEvent.click(screen.getByRole("button", { name: "Send reset link" }));

    expect(await screen.findByRole("heading", { name: "Reset link sent" })).toBeInTheDocument();
    expect(requestPasswordResetMock).toHaveBeenCalledWith("demo@example.test");
    expect(screen.getByText(/If an account uses demo@example.test/)).toBeInTheDocument();
  });

  it("asks for a valid email before calling the server", () => {
    render(<ForgotPasswordPage />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "not-an-email" } });
    fireEvent.click(screen.getByRole("button", { name: "Send reset link" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Enter the email address of your account.");
    expect(requestPasswordResetMock).not.toHaveBeenCalled();
  });

  it("shows the server error when the request fails", async () => {
    requestPasswordResetMock.mockRejectedValue(new Error("Service unavailable"));
    render(<ForgotPasswordPage />);

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "demo@example.test" } });
    fireEvent.click(screen.getByRole("button", { name: "Send reset link" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Service unavailable");
  });

  it("goes back to sign in", () => {
    render(<ForgotPasswordPage />);
    fireEvent.click(screen.getByRole("button", { name: "Back to sign in" }));
    expect(window.location.pathname).toBe("/");
  });
});

describe("ResetPasswordPage", () => {
  beforeEach(() => {
    resetPasswordMock.mockReset();
    window.history.replaceState({}, "", "/reset-password?token=abc123");
  });

  it("sets the new password with the emailed token and removes the token from the address", async () => {
    resetPasswordMock.mockResolvedValue(undefined);
    const onDone = vi.fn();
    render(<ResetPasswordPage onDone={onDone} />);
    expect(window.location.search).toBe("");

    fireEvent.change(screen.getByLabelText("New password"), { target: { value: "brand-new-password" } });
    fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "brand-new-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Set new password" }));

    await waitFor(() => expect(onDone).toHaveBeenCalledWith(expect.stringMatching(/password has been changed/)));
    expect(resetPasswordMock).toHaveBeenCalledWith("abc123", "brand-new-password");
    expect(window.location.pathname).toBe("/");
  });

  it("checks the passwords before calling the server", () => {
    render(<ResetPasswordPage onDone={vi.fn()} />);

    fireEvent.change(screen.getByLabelText("New password"), { target: { value: "brand-new-password" } });
    fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "other-password-1" } });
    fireEvent.click(screen.getByRole("button", { name: "Set new password" }));

    expect(screen.getByRole("alert")).toHaveTextContent("The two passwords do not match.");
    expect(resetPasswordMock).not.toHaveBeenCalled();
  });

  it("offers a new link when the token has expired", async () => {
    resetPasswordMock.mockRejectedValue(new Error("This reset link is invalid or has expired. Ask for a new one."));
    render(<ResetPasswordPage onDone={vi.fn()} />);

    fireEvent.change(screen.getByLabelText("New password"), { target: { value: "brand-new-password" } });
    fireEvent.change(screen.getByLabelText("Confirm new password"), { target: { value: "brand-new-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Set new password" }));

    fireEvent.click(await screen.findByRole("button", { name: "Ask for a new link" }));
    expect(window.location.pathname).toBe("/forgot-password");
  });

  it("explains a link without a token", () => {
    window.history.replaceState({}, "", "/reset-password");
    render(<ResetPasswordPage onDone={vi.fn()} />);
    expect(screen.getByRole("heading", { name: "This link is incomplete" })).toBeInTheDocument();
  });
});
