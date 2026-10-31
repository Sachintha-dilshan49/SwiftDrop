import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthApp } from "./AuthApp";

const { loginMock, signupMock, exchangeGoogleCodeMock, saveSessionMock, logoutMock } = vi.hoisted(() => ({
  loginMock: vi.fn(),
  signupMock: vi.fn(),
  exchangeGoogleCodeMock: vi.fn(),
  logoutMock: vi.fn(),
  saveSessionMock: vi.fn((tokens: {
    accessToken: string;
    refreshToken: string;
    user: { id: string; fullName: string; email: string; roles: string[] };
  }) => ({
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    user: tokens.user,
  })),
}));

vi.mock("./authApi", () => ({
  login: (...args: unknown[]) => loginMock(...args),
  signup: (...args: unknown[]) => signupMock(...args),
  exchangeGoogleCode: (...args: unknown[]) => exchangeGoogleCodeMock(...args),
  googleLoginEnabled: false,
  logout: (...args: unknown[]) => logoutMock(...args),
  readSession: vi.fn(() => null),
  saveSession: (tokens: Parameters<typeof saveSessionMock>[0]) => saveSessionMock(tokens),
  clearSession: vi.fn(),
  startGoogleLogin: vi.fn(),
}));

describe("AuthApp", () => {
  beforeEach(() => {
    loginMock.mockReset();
    signupMock.mockReset();
    exchangeGoogleCodeMock.mockReset();
    logoutMock.mockReset();
    saveSessionMock.mockClear();
    window.history.replaceState({}, "", "/");
  });

  it("submits valid login details and opens the customer's dashboard", async () => {
    loginMock.mockResolvedValue({
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: "1", fullName: "Demo Customer", email: "demo@example.test", roles: ["CUSTOMER"] },
    });
    render(<AuthApp />);

    fireEvent.change(screen.getByLabelText("Email or mobile"), { target: { value: "demo@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "a-very-long-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Sign in to Customer portal" }));

    await waitFor(() =>
      expect(screen.getByRole("heading", { name: "Good morning, Demo" })).toBeInTheDocument(),
    );
    expect(window.location.pathname).toBe("/customer");
    expect(loginMock).toHaveBeenCalledWith({ emailOrMobile: "demo@example.test", password: "a-very-long-password" });
  });

  it.each([
    ["Merchant", ["MERCHANT"], "/merchant", "Merchant portal"],
    ["Courier", ["COURIER"], "/courier", "Courier portal"],
    ["Staff", ["DISPATCHER"], "/dispatch", "Dispatch portal"],
    ["Staff", ["ADMIN"], "/admin", "Admin portal"],
  ])("opens the %s dashboard for the matching role", async (portal, roles, path, portalName) => {
    loginMock.mockResolvedValue({
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: "1", fullName: "Demo User", email: "demo@example.test", roles },
    });
    render(<AuthApp />);

    fireEvent.click(screen.getByRole("button", { name: portal }));
    fireEvent.change(screen.getByLabelText("Email or mobile"), { target: { value: "demo@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "correct-password" } });
    fireEvent.click(screen.getByRole("button", { name: `Sign in to ${portal} portal` }));

    expect(await screen.findByRole("banner")).toHaveTextContent(portalName);
    expect(window.location.pathname).toBe(path);
  });

  it("displays the API error when login fails", async () => {
    loginMock.mockRejectedValue(new Error("Email or password is invalid."));
    render(<AuthApp />);

    fireEvent.change(screen.getByLabelText("Email or mobile"), { target: { value: "demo@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "incorrect-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Sign in to Customer portal" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Email or password is invalid.");
  });

  it("requires a 12-character password for signup", () => {
    render(<AuthApp />);
    fireEvent.click(screen.getByRole("button", { name: "Create an account" }));
    expect(screen.getByLabelText("Password")).toHaveAttribute("minLength", "12");
    expect(screen.getByLabelText("Full name")).toBeRequired();
    expect(screen.getByLabelText("Mobile number")).toBeRequired();
    expect(screen.getByLabelText("I agree to the terms and the privacy policy")).toBeRequired();
  });

  it("opens the customer dashboard after signup", async () => {
    signupMock.mockResolvedValue({
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: "1", fullName: "Demo Customer", email: "demo@example.test", roles: ["CUSTOMER"] },
    });
    render(<AuthApp />);

    fireEvent.click(screen.getByRole("button", { name: "Create an account" }));
    fireEvent.change(screen.getByLabelText("Full name"), { target: { value: "Demo Customer" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "demo@example.test" } });
    fireEvent.change(screen.getByLabelText("Mobile number"), { target: { value: "+94771234567" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "a-very-long-password" } });
    fireEvent.click(screen.getByLabelText("I agree to the terms and the privacy policy"));
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("heading", { name: "Good morning, Demo" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/customer");
  });

  it("opens the role dashboard after a Google login callback", async () => {
    exchangeGoogleCodeMock.mockResolvedValue({
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: "1", fullName: "Demo Customer", email: "demo@example.test", roles: ["CUSTOMER"] },
    });
    window.history.replaceState({}, "", "/auth/callback?code=oauth-code");
    render(<AuthApp />);

    expect(await screen.findByRole("heading", { name: "Good morning, Demo" })).toBeInTheDocument();
    expect(window.location.pathname).toBe("/customer");
    expect(exchangeGoogleCodeMock).toHaveBeenCalledWith("oauth-code");
  });

  it("revokes a successful login if the chosen portal does not match the account roles", async () => {
    loginMock.mockResolvedValue({
      accessToken: "access",
      refreshToken: "refresh",
      user: { id: "1", fullName: "Demo Customer", email: "demo@example.test", roles: ["CUSTOMER"] },
    });
    render(<AuthApp />);

    fireEvent.click(screen.getByRole("button", { name: "Merchant" }));
    fireEvent.change(screen.getByLabelText("Email or mobile"), { target: { value: "demo@example.test" } });
    fireEvent.change(screen.getByLabelText("Password"), { target: { value: "correct-password" } });
    fireEvent.click(screen.getByRole("button", { name: "Sign in to Merchant portal" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "This account doesn’t have access to the Merchant portal.",
    );
    expect(logoutMock).toHaveBeenCalledWith("refresh");
    expect(saveSessionMock).not.toHaveBeenCalled();
  });
});
