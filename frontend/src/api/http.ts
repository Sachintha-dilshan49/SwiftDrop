// Shared fetch for calls that send the user's token. Access tokens last 15 minutes (architecture §9), so on a
// 401 this asks auth-service for new tokens once, saves them and retries the request.
import { AuthSession, clearSession, readSession, refreshTokens, saveSession } from "../auth/authApi";

/** Fired on window when the refresh token no longer works; AuthApp then shows the sign-in page. */
export const SESSION_ENDED_EVENT = "swiftdrop:session-ended";

type AuthorizedInit = Omit<RequestInit, "headers"> & { headers?: Record<string, string> };

// One refresh at a time: requests that get a 401 together share it (the refresh token is rotated on use).
let refreshing: Promise<AuthSession | null> | null = null;

function refreshSession(refreshToken: string): Promise<AuthSession | null> {
  if (!refreshing) {
    refreshing = refreshTokens(refreshToken)
      .then(saveSession)
      .catch(() => null)
      .finally(() => {
        refreshing = null;
      });
  }
  return refreshing;
}

function withToken(init: AuthorizedInit, accessToken: string): AuthorizedInit {
  return { ...init, headers: { ...init.headers, Authorization: `Bearer ${accessToken}` } };
}

/**
 * fetch for API calls with an Authorization header. It always sends the latest stored access token (screens
 * may still hold an older one), and on a 401 refreshes the tokens once and retries. If that fails, the
 * session is cleared, SESSION_ENDED_EVENT is fired and the original 401 response is returned, so callers
 * show their usual "session expired" message. Requests without an Authorization header are sent unchanged.
 */
export async function authorizedFetch(url: string, init: AuthorizedInit = {}): Promise<Response> {
  const session = readSession();
  if (!session || !init.headers?.Authorization) {
    return fetch(url, init);
  }

  const response = await fetch(url, withToken(init, session.accessToken));
  if (response.status !== 401) {
    return response;
  }

  // Another request may already have refreshed while this one was on its way.
  const latest = readSession();
  if (latest && latest.accessToken !== session.accessToken) {
    return fetch(url, withToken(init, latest.accessToken));
  }

  const renewed = await refreshSession(session.refreshToken);
  if (!renewed) {
    clearSession();
    window.dispatchEvent(new Event(SESSION_ENDED_EVENT));
    return response;
  }
  return fetch(url, withToken(init, renewed.accessToken));
}
