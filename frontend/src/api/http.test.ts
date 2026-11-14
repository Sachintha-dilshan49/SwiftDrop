import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { readSession } from "../auth/authApi";
import { SESSION_ENDED_EVENT, authorizedFetch } from "./http";

const user = { id: "1", fullName: "Demo Customer", email: "demo@example.test", roles: ["CUSTOMER"] };
const REFRESH_URL = "/api/v1/auth/refresh";

function storeSession(accessToken: string, refreshToken: string) {
  sessionStorage.setItem("swiftdrop.auth.session", JSON.stringify({ accessToken, refreshToken, user }));
}

function json(status: number, body: unknown = {}) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function tokens(accessToken: string, refreshToken: string) {
  return json(200, { accessToken, refreshToken, tokenType: "Bearer", accessTokenExpiresAt: "t", user });
}

type Call = [string, RequestInit];
const authHeader = (call: Call) => (call[1].headers as Record<string, string>).Authorization;

describe("authorizedFetch", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    sessionStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends the request unchanged when nobody is signed in", async () => {
    fetchMock.mockResolvedValue(json(200));

    await authorizedFetch("/api/v1/parcels", { headers: { Authorization: "Bearer from-screen" } });

    expect(authHeader(fetchMock.mock.calls[0] as Call)).toBe("Bearer from-screen");
  });

  it("always sends the latest stored access token, even if the screen passed an older one", async () => {
    storeSession("access-2", "refresh-2");
    fetchMock.mockResolvedValue(json(200));

    await authorizedFetch("/api/v1/parcels", {
      method: "GET",
      headers: { Authorization: "Bearer access-1", Accept: "x" },
    });

    const [url, init] = fetchMock.mock.calls[0] as Call;
    expect(url).toBe("/api/v1/parcels");
    expect(init).toMatchObject({ method: "GET", headers: { Authorization: "Bearer access-2", Accept: "x" } });
  });

  it("leaves requests without a token alone", async () => {
    storeSession("access-1", "refresh-1");
    fetchMock.mockResolvedValue(json(200));

    await authorizedFetch("/api/v1/merchants/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    });

    expect((fetchMock.mock.calls[0] as Call)[1].headers).toEqual({ "Content-Type": "application/json" });
  });

  it("refreshes the tokens once on a 401 and retries with the new access token", async () => {
    storeSession("access-1", "refresh-1");
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockResolvedValueOnce(tokens("access-2", "refresh-2"))
      .mockResolvedValueOnce(json(200, { ok: true }));

    const response = await authorizedFetch("/api/v1/parcels", { headers: { Authorization: "Bearer access-1" } });

    expect(response.status).toBe(200);
    const [refreshUrl, refreshInit] = fetchMock.mock.calls[1] as Call;
    expect(refreshUrl).toBe(REFRESH_URL);
    expect(JSON.parse(refreshInit.body as string)).toEqual({ refreshToken: "refresh-1" });
    expect(authHeader(fetchMock.mock.calls[2] as Call)).toBe("Bearer access-2");
    expect(readSession()).toMatchObject({ accessToken: "access-2", refreshToken: "refresh-2" });
  });

  it("shares one refresh between requests that get a 401 at the same time", async () => {
    storeSession("access-1", "refresh-1");
    fetchMock.mockImplementation(async (url: string, init: RequestInit) => {
      if (url === REFRESH_URL) return tokens("access-2", "refresh-2");
      return authHeader([url, init]) === "Bearer access-2" ? json(200) : json(401);
    });

    const results = await Promise.all([
      authorizedFetch("/api/v1/parcels", { headers: { Authorization: "Bearer access-1" } }),
      authorizedFetch("/api/v1/courier/deliveries/today", { headers: { Authorization: "Bearer access-1" } }),
    ]);

    expect(results.map((response) => response.status)).toEqual([200, 200]);
    expect(fetchMock.mock.calls.filter(([url]) => url === REFRESH_URL)).toHaveLength(1);
  });

  it("retries with a token another request already refreshed instead of refreshing again", async () => {
    storeSession("access-1", "refresh-1");
    fetchMock.mockImplementationOnce(async () => {
      storeSession("access-2", "refresh-2"); // another request finished its refresh meanwhile
      return json(401);
    });
    fetchMock.mockResolvedValueOnce(json(200));

    const response = await authorizedFetch("/api/v1/parcels", { headers: { Authorization: "Bearer access-1" } });

    expect(response.status).toBe(200);
    expect(fetchMock.mock.calls.some(([url]) => url === REFRESH_URL)).toBe(false);
    expect(authHeader(fetchMock.mock.calls[1] as Call)).toBe("Bearer access-2");
  });

  it("ends the session when the refresh token no longer works", async () => {
    storeSession("access-1", "refresh-1");
    fetchMock.mockResolvedValueOnce(json(401)).mockResolvedValueOnce(json(401, { detail: "Refresh token revoked" }));
    const ended = vi.fn();
    window.addEventListener(SESSION_ENDED_EVENT, ended);

    const response = await authorizedFetch("/api/v1/parcels", { headers: { Authorization: "Bearer access-1" } });

    window.removeEventListener(SESSION_ENDED_EVENT, ended);
    expect(response.status).toBe(401);
    expect(readSession()).toBeNull();
    expect(ended).toHaveBeenCalledTimes(1);
  });
});
