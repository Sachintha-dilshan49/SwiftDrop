import { FormEvent, ReactNode, useEffect, useState } from "react";
import { requestPasswordReset, resetPassword } from "./authApi";
import { navigate } from "../routes/router";
import { newPasswordError } from "./newPassword";


function errorText(error: unknown): string {
  return error instanceof Error && error.message ? error.message : "Something went wrong. Please try again.";
}

function ResetCard({ eyebrow, title, children }: { eyebrow: string; title: string; children: ReactNode }) {
  return (
    <main className="page-shell">
      <section className="signed-in-card reset-card">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {children}
      </section>
    </main>
  );
}

/** Asks for the account email and sends a reset link. */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = email.trim();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError("Enter the email address of your account.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await requestPasswordReset(trimmed);
      setSent(true);
    } catch (err: unknown) {
      setError(errorText(err));
    } finally {
      setBusy(false);
    }
  }

  if (sent) {
    return (
      <ResetCard eyebrow="CHECK YOUR EMAIL" title="Reset link sent">
        <p className="muted">
          If an account uses {email.trim()}, we have emailed a link to reset its password. The link works once and
          expires in 30 minutes.
        </p>
        <button className="button button-outline" type="button" onClick={() => navigate("/")}>
          Back to sign in
        </button>
      </ResetCard>
    );
  }

  return (
    <ResetCard eyebrow="FORGOT PASSWORD" title="Reset your password">
      <p className="muted">Enter your account email and we will send you a link to choose a new password.</p>
      {error && <div className="alert" role="alert">{error}</div>}
      <form className="auth-form" onSubmit={submit} noValidate>
        <label htmlFor="resetEmail">Email</label>
        <input
          id="resetEmail"
          type="email"
          autoComplete="email"
          placeholder="you@example.com"
          maxLength={255}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <button className="button button-primary" type="submit" disabled={busy}>
          {busy ? "Sending…" : "Send reset link"}
        </button>
      </form>
      <button className="forgot-link reset-back" type="button" onClick={() => navigate("/")}>
        Back to sign in
      </button>
    </ResetCard>
  );
}

/** Opened from the emailed link (/reset-password?token=...). */
export function ResetPasswordPage({ onDone }: { onDone: (message: string) => void }) {
  const [token] = useState(() => new URLSearchParams(window.location.search).get("token") ?? "");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    // Keep the single-use token out of the address bar and browser history
    if (window.location.search) {
      window.history.replaceState(null, "", "/reset-password");
    }
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const problem = newPasswordError(password, confirmation);
    if (problem) {
      setError(problem);
      return;
    }
    setBusy(true);
    setError("");
    try {
      await resetPassword(token, password);
      navigate("/", { replace: true });
      onDone("Your password has been changed. Sign in with your new password.");
    } catch (err: unknown) {
      setError(errorText(err));
      setBusy(false);
    }
  }

  if (!token) {
    return (
      <ResetCard eyebrow="RESET PASSWORD" title="This link is incomplete">
        <p className="muted">Open the link from your email again, or ask for a new one.</p>
        <button className="button button-primary" type="button" onClick={() => navigate("/forgot-password")}>
          Ask for a new link
        </button>
      </ResetCard>
    );
  }

  return (
    <ResetCard eyebrow="RESET PASSWORD" title="Choose a new password">
      <p className="muted">You will be signed out on every device that used your old password.</p>
      {error && (
        <div className="alert" role="alert">
          {error}
          {/expired|invalid/i.test(error) && (
            <>
              {" "}
              <button className="forgot-link reset-inline" type="button" onClick={() => navigate("/forgot-password")}>
                Ask for a new link
              </button>
            </>
          )}
        </div>
      )}
      <form className="auth-form" onSubmit={submit} noValidate>
        <label htmlFor="newPassword">New password</label>
        <input
          id="newPassword"
          type="password"
          autoComplete="new-password"
          placeholder="At least 12 characters"
          maxLength={72}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <label htmlFor="confirmPassword">Confirm new password</label>
        <input
          id="confirmPassword"
          type="password"
          autoComplete="new-password"
          maxLength={72}
          value={confirmation}
          onChange={(event) => setConfirmation(event.target.value)}
        />
        <button className="button button-primary" type="submit" disabled={busy}>
          {busy ? "Saving…" : "Set new password"}
        </button>
      </form>
    </ResetCard>
  );
}
