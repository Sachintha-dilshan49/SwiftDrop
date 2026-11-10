export type AuthUser = {
  id: string;
  fullName: string;
  email: string;
  roles: string[];
};

export type TokenResponse = {
  accessToken: string;
  tokenType: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  user: AuthUser;
};

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";
const AUTH_BASE_URL = import.meta.env.VITE_AUTH_BASE_URL ?? "http://localhost:8081";
const SESSION_KEY = "swiftdrop.auth.session";

async function post<TRequest, TResponse>(
  path: string,
  request: TRequest,
): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}/v1/auth${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { detail?: string; title?: string }
      | null;
    throw new Error(body?.detail ?? body?.title ?? "Unable to complete sign in.");
  }
  if (response.status === 204) {
    return undefined as TResponse;
  }
  return (await response.json()) as TResponse;
}

export function signup(input: {
  fullName: string;
  email: string;
  mobileNumber?: string;
  password: string;
}): Promise<TokenResponse> {
  return post("/signup", input);
}

export function login(input: {
  emailOrMobile: string;
  password: string;
}): Promise<TokenResponse> {
  return post("/login", input);
}

export function exchangeGoogleCode(code: string): Promise<TokenResponse> {
  return post("/token", { code });
}

export async function logout(refreshToken: string): Promise<void> {
  await post("/logout", { refreshToken });
}

export function saveSession(tokens: TokenResponse): AuthSession {
  const session: AuthSession = {
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    user: tokens.user,
  };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function readSession(): AuthSession | null {
  const stored = sessionStorage.getItem(SESSION_KEY);
  if (!stored) return null;
  try {
    return JSON.parse(stored) as AuthSession;
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY);
}

export function startGoogleLogin(): void {
  window.location.assign(`${AUTH_BASE_URL}/oauth2/authorization/google`);
}

export const googleLoginEnabled =
  import.meta.env.VITE_GOOGLE_LOGIN_ENABLED === "true";
