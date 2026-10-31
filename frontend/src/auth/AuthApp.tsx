import { FormEvent, useEffect, useState } from "react";
import {
  AuthSession,
  clearSession,
  exchangeGoogleCode,
  googleLoginEnabled,
  login,
  logout,
  readSession,
  saveSession,
  signup,
} from "./authApi";
import {
  AuthPortalId,
  authPortals,
  defaultDashboardRole,
  dashboardPathForPortal,
  dashboardRoleForPath,
  dashboardPathForRoles,
  portalLabel,
  userHasPortalRole,
} from "./authPortals";
import { RoleDashboard } from "../dashboard/RoleDashboard";

type AuthMode = "login" | "signup";

export function AuthApp() {
  const [session, setSession] = useState<AuthSession | null>(readSession);
  const [mode, setMode] = useState<AuthMode>("login");
  const [portal, setPortal] = useState<AuthPortalId>("customer");
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState<"error" | "info">("error");
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [, setRouteVersion] = useState(0);
  const isCallback = window.location.pathname === "/auth/callback";

  useEffect(() => {
    const handlePopState = () => setRouteVersion((version) => version + 1);
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    if (!isCallback) return;
    const query = new URLSearchParams(window.location.search);
    const code = query.get("code");
    const oauthError = query.get("error");
    window.history.replaceState(null, "", "/auth/callback");
    if (oauthError || !code) {
      setMessage("Google sign in did not complete. Please try again.");
      return;
    }

    setBusy(true);
    exchangeGoogleCode(code)
      .then((tokens) => {
        const destination = dashboardPathForRoles(tokens.user.roles);
        if (!destination) {
          throw new Error("This account does not have a SwiftDrop portal assigned.");
        }
        const savedSession = saveSession(tokens);
        window.history.replaceState(null, "", destination);
        setSession(savedSession);
        setMessage("");
      })
      .catch((error: unknown) => {
        window.history.replaceState(null, "", "/");
        setMessage(errorMessage(error));
      })
      .finally(() => setBusy(false));
  }, [isCallback]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    const form = new FormData(event.currentTarget);
    try {
      const tokens =
        mode === "login"
          ? await login({
              emailOrMobile: String(form.get("emailOrMobile")),
              password: String(form.get("password")),
            })
          : await signup({
              fullName: String(form.get("fullName")),
              email: String(form.get("email")),
              mobileNumber: String(form.get("mobileNumber")),
              password: String(form.get("password")),
            });

      if (mode === "login" && !userHasPortalRole(portal, tokens.user.roles)) {
        await logout(tokens.refreshToken);
        setMessage(`This account doesn’t have access to the ${portalLabel(portal)} portal.`);
        return;
      }
      const destination =
        mode === "login"
          ? dashboardPathForPortal(portal, tokens.user.roles)
          : dashboardPathForRoles(tokens.user.roles);
      if (!destination) {
        await logout(tokens.refreshToken);
        setMessage("This account does not have a SwiftDrop portal assigned.");
        return;
      }
      const savedSession = saveSession(tokens);
      window.history.pushState(null, "", destination);
      setSession(savedSession);
    } catch (error: unknown) {
      setMessage(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function signOut() {
    if (session) {
      try {
        await logout(session.refreshToken);
      } catch {
        setMessage("Your session was cleared, but the server could not confirm sign out.");
      }
    }
    clearSession();
    setSession(null);
    window.history.replaceState(null, "", "/");
  }

  function showRecoveryNotice() {
    setMessage("Password recovery is not available yet. Please contact SwiftDrop support.");
    setMessageType("info");
  }

  if (busy && isCallback) {
    return (
      <main className="callback-state">
        <BrandLockup />
        <p>Finishing Google sign in…</p>
        <span className="loading-indicator" aria-hidden="true" />
      </main>
    );
  }

  if (session) {
    const pathRole = dashboardRoleForPath(window.location.pathname);
    const dashboardRole =
      pathRole && session.user.roles.includes(pathRole)
        ? pathRole
        : defaultDashboardRole(session.user.roles);
    const destination = dashboardRole ? dashboardPathForRoles([dashboardRole]) : null;
    if (destination && destination !== window.location.pathname) {
      window.history.replaceState(null, "", destination);
    }
    if (dashboardRole) {
      return <RoleDashboard role={dashboardRole} user={session.user} onSignOut={signOut} />;
    }
    return (
      <main className="page-shell">
        <section className="signed-in-card">
          <BrandLockup />
          <div className="signed-in-icon" aria-hidden="true"><CheckIcon /></div>
          <p className="eyebrow">SWIFTDROP ACCOUNT</p>
          <h1>Welcome, {session.user.fullName}</h1>
          <p className="muted">{session.user.email}</p>
          <div className="role-list" aria-label="Your roles">
            {session.user.roles.map((role) => <span key={role}>{role}</span>)}
          </div>
          <button className="button button-outline" onClick={signOut}>Sign out</button>
        </section>
      </main>
    );
  }

  return (
    <main className="page-shell">
      <section className={`auth-layout ${mode === "signup" ? "is-signup" : ""}`}>
        <aside className="brand-panel">
          <BrandLockup />
          <div className="brand-copy">
            <h2>Send anything across Colombo in under an hour.</h2>
            <p className="brand-intro">
              One platform for customers, merchants, couriers and staff.
            </p>
            <ul className="benefit-list">
              <li><BoltIcon /> Couriers in every Colombo zone, 6 AM to 11 PM</li>
              <li><StoreIcon /> Merchants ship orders by dashboard or API</li>
              <li><SparkIcon /> Customers book from one message with the AI assistant</li>
            </ul>
          </div>
          <p className="brand-footnote">SwiftDrop makes every delivery feel effortless.</p>
        </aside>

        <section className="auth-panel">
          <div className="auth-content">
            <p className="eyebrow">{mode === "login" ? "GOOD TO SEE YOU" : "GET STARTED"}</p>
            <h1>{mode === "login" ? "Welcome back" : "Create your account"}</h1>
            <p className="muted">
              {mode === "login"
                ? "Choose your portal and sign in."
                : "Book deliveries and track them live. Merchants register from the Merchant portal."}
            </p>

            {message && (
              <div className={`alert ${messageType === "info" ? "alert-info" : ""}`} role="alert">
                {message}
              </div>
            )}

            {mode === "login" && (
              <div className="portal-picker" role="group" aria-label="Choose your portal">
                {authPortals.map((option) => (
                  <button
                    className={portal === option.id ? "is-selected" : ""}
                    key={option.id}
                    type="button"
                    aria-pressed={portal === option.id}
                    onClick={() => {
                      setPortal(option.id);
                      setMessage("");
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            )}

            <button
              className="button button-google"
              type="button"
              onClick={() => {
                if (googleLoginEnabled) {
                  window.location.assign(
                    `${import.meta.env.VITE_AUTH_BASE_URL ?? "http://localhost:8081"}/oauth2/authorization/google`,
                  );
                }
              }}
              disabled={!googleLoginEnabled || busy}
              title={googleLoginEnabled ? "Continue with Google" : "Google login is not configured"}
            >
              <GoogleIcon />
              {mode === "signup" ? "Sign up with Google" : "Continue with Google"}
            </button>
            {!googleLoginEnabled && (
              <p className="google-note">Google sign-in is currently unavailable.</p>
            )}

            <div className="divider"><span>or use email</span></div>

            <form className="auth-form" onSubmit={submit}>
              {mode === "signup" && (
                <>
                  <label htmlFor="fullName">Full name</label>
                  <input
                    id="fullName"
                    name="fullName"
                    autoComplete="name"
                    placeholder="Your full name"
                    required
                    maxLength={150}
                  />
                </>
              )}
              {mode === "login" ? (
                <>
                  <label htmlFor="emailOrMobile">Email or mobile</label>
                  <input
                    id="emailOrMobile"
                    name="emailOrMobile"
                    autoComplete="username"
                    placeholder="Email address or mobile number"
                    required
                    maxLength={255}
                  />
                </>
              ) : (
                <>
                  <label htmlFor="email">Email</label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    autoComplete="email"
                    placeholder="you@example.com"
                    required
                    maxLength={255}
                  />
                  <label htmlFor="mobileNumber">Mobile number</label>
                  <input
                    id="mobileNumber"
                    name="mobileNumber"
                    type="tel"
                    autoComplete="tel"
                    placeholder="+94 71 234 5678"
                    required
                    maxLength={30}
                  />
                  <p className="field-hint">We text tracking links here. It is never shown to couriers in full.</p>
                </>
              )}

              <label htmlFor="password">Password</label>
              <div className="password-field">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete={mode === "login" ? "current-password" : "new-password"}
                  minLength={mode === "signup" ? 12 : 1}
                  maxLength={72}
                  placeholder={mode === "signup" ? "At least 12 characters" : "Enter your password"}
                  required
                />
                <button
                  type="button"
                  className="password-toggle"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
              {mode === "signup" ? (
                <>
                  <p className="field-hint">At least 12 characters, including a number.</p>
                  <label className="terms-check">
                    <input
                      type="checkbox"
                      checked={acceptedTerms}
                      onChange={(event) => setAcceptedTerms(event.target.checked)}
                      required
                    />
                    <span>I agree to the terms and the privacy policy</span>
                  </label>
                </>
              ) : (
                <button className="forgot-link" type="button" onClick={showRecoveryNotice}>
                  Forgot password?
                </button>
              )}
              <button
                className="button button-primary"
                type="submit"
                disabled={busy || (mode === "signup" && !acceptedTerms)}
              >
                {busy
                  ? "Please wait…"
                  : mode === "signup"
                    ? "Create account"
                    : `Sign in to ${portalLabel(portal)} portal`}
              </button>
            </form>

            <p className="switch-mode">
              {mode === "login" ? "New to SwiftDrop?" : "Already have an account?"}{" "}
              <button
                type="button"
                onClick={() => {
                  setMode(mode === "login" ? "signup" : "login");
                  setMessage("");
                  setAcceptedTerms(false);
                  setShowPassword(false);
                }}
              >
                {mode === "login" ? "Create an account" : "Sign in"}
              </button>
            </p>
            {mode === "login" && (
              <p className="portal-note">
                Google sign-in is for customers. Couriers and staff use the account SwiftDrop gives them.
              </p>
            )}
          </div>
        </section>
      </section>
    </main>
  );
}

function BrandLockup() {
  return (
    <div className="brand-lockup">
      <span className="brand-mark" aria-hidden="true"><BrandIcon /></span>
      <span>SwiftDrop</span>
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg className="google-icon" viewBox="0 0 24 24" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.27-4.74 3.27-8.1Z" />
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.65l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.28-1.93-6.15-4.53H2.16v2.84A11 11 0 0 0 12 23Z" />
      <path fill="#FBBC05" d="M5.85 14.11a6.6 6.6 0 0 1 0-4.22V7.05H2.16a11 11 0 0 0 0 9.9l3.69-2.84Z" />
      <path fill="#EA4335" d="M12 5.36c1.62 0 3.06.56 4.2 1.64l3.15-3.15C17.45 2.07 14.97 1 12 1a11 11 0 0 0-9.84 6.05l3.69 2.84c.87-2.6 3.29-4.53 6.15-4.53Z" />
    </svg>
  );
}

function BrandIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 12h12m-5-5 5 5-5 5" />
      <path d="M12 3.5a8.5 8.5 0 1 0 8.5 8.5" />
    </svg>
  );
}

function BoltIcon() {
  return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m11 1-7 10h5l-1 8 8-11h-5l1-7Z" /></svg>;
}

function StoreIcon() {
  return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M3 8v9h14V8M2 8l2-5h12l2 5M2 8a2 2 0 0 0 4 0 2 2 0 0 0 4 0 2 2 0 0 0 4 0 2 2 0 0 0 4 0M8 17v-5h4v5" /></svg>;
}

function SparkIcon() {
  return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m10 2 1.6 5.2L17 9l-5.4 1.8L10 16l-1.6-5.2L3 9l5.4-1.8L10 2ZM16 13l.8 2.2L19 16l-2.2.8L16 19l-.8-2.2L13 16l2.2-.8L16 13Z" /></svg>;
}

function EyeIcon() {
  return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M2 10s2.8-5 8-5 8 5 8 5-2.8 5-8 5-8-5-8-5Z" /><circle cx="10" cy="10" r="2" /></svg>;
}

function EyeOffIcon() {
  return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m3 3 14 14M8.2 5.2A8.8 8.8 0 0 1 10 5c5.2 0 8 5 8 5a13 13 0 0 1-2.3 2.8M5.5 5.5C3.2 6.8 2 10 2 10s2.8 5 8 5c.8 0 1.6-.1 2.3-.4" /><path d="M8.6 8.6a2 2 0 0 0 2.8 2.8" /></svg>;
}

function CheckIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>;
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Unable to complete sign in.";
}
