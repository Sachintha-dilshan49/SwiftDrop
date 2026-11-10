import { afterEach, describe, expect, it, vi } from "vitest";
import { DispatchApiError, dispatchApi } from "./dispatchApi";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

describe("dispatchApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("reads the unassigned parcels with the dispatcher's token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, [{ trackingNo: "SD-0001" }]));
    vi.stubGlobal("fetch", fetchMock);

    await expect(dispatchApi.getUnassignedParcels("token-1")).resolves.toEqual([{ trackingNo: "SD-0001" }]);

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/dispatch/unassigned", {
      headers: { Accept: "application/json", Authorization: "Bearer token-1" },
    });
  });

  it("posts the parcel and courier to assign", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(201, { id: "a-1" }));
    vi.stubGlobal("fetch", fetchMock);

    await dispatchApi.assign("p-1", "c-1", "token-1");

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("/api/v1/dispatch/assignments");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ parcelId: "p-1", courierId: "c-1" });
    expect((init.headers as Record<string, string>).Authorization).toBe("Bearer token-1");
  });

  it("uses the ProblemDetail detail as the error message", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(409, {
      type: "https://swiftdrop.lk/problems/courier-unavailable", status: 409, detail: "Courier is offline.",
    })));

    const error = await dispatchApi.assign("p-1", "c-1", "t").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(DispatchApiError);
    expect(error).toMatchObject({ status: 409, message: "Courier is offline." });
  });

  it("falls back to a friendly message when the reply is not a ProblemDetail", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("Bad gateway", { status: 502 })));

    await expect(dispatchApi.getWorkload("t")).rejects.toMatchObject({
      status: 502, message: "Could not load the courier workload.",
    });
  });
});
