import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DailyReport } from "../../api/dispatchApi";
import { ReportsTab } from "./ReportsTab";
import { rangeStart, successRate } from "./dispatchFormat";

const REPORT: DailyReport = {
  from: "2026-11-08",
  to: "2026-11-14",
  days: [
    { date: "2026-11-13", assigned: 6, delivered: 5, failed: 1 },
    { date: "2026-11-14", assigned: 4, delivered: 3, failed: 0 },
  ],
  totals: { assigned: 10, delivered: 8, failed: 1 },
  couriers: [
    { courierId: "c1", firstName: "Kasun", assigned: 7, delivered: 6, failed: 1 },
    { courierId: "c2a1b2c3d4", firstName: null, assigned: 3, delivered: 2, failed: 0 },
  ],
};

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

describe("ReportsTab", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows totals, success rate and each courier's share", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(json(200, REPORT)));
    render(<ReportsTab token="t" />);

    expect(await screen.findByText("Success rate", { selector: ".dispatch-stat span" })).toBeInTheDocument();
    const stats = screen.getAllByText(/^(10|8|1|89%)$/, { selector: ".dispatch-stat strong" });
    expect(stats.map((s) => s.textContent)).toEqual(["10", "8", "1", "89%"]);
    const couriers = screen.getAllByRole("table")[1];
    expect(within(couriers).getByText("Kasun")).toBeInTheDocument();
    expect(within(couriers).getByText("Courier c2a1b2c3")).toBeInTheDocument();
    expect(within(couriers).getByText("86%")).toBeInTheDocument();
  });

  it("asks for the last 7 days and reloads for 30", async () => {
    // A fresh Response per call: a body can only be read once
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(json(200, REPORT)));
    vi.stubGlobal("fetch", fetchMock);
    render(<ReportsTab token="token-1" />);
    await screen.findByText("Kasun");

    await userEvent.click(screen.getByRole("button", { name: "Last 30 days" }));
    await screen.findByText("Kasun");

    const urls = fetchMock.mock.calls.map(([url]) => String(url));
    expect(urls[0]).toMatch(/\/api\/v1\/reports\/daily\?from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}$/);
    expect(urls[1]).toContain(`from=${rangeStart(30)}`);
    expect((fetchMock.mock.calls[0][1] as RequestInit).headers).toMatchObject({ Authorization: "Bearer token-1" });
    expect(screen.getByRole("button", { name: "Last 30 days" })).toHaveAttribute("aria-pressed", "true");
  });

  it("explains an expired session", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(json(401, { detail: "Jwt expired" })));
    render(<ReportsTab token="t" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("Your session has expired");
  });

  it("says so when nobody delivered anything", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(json(200, { ...REPORT, couriers: [],
      totals: { assigned: 0, delivered: 0, failed: 0 } })));
    render(<ReportsTab token="t" />);

    expect(await screen.findByText("No deliveries in this period.")).toBeInTheDocument();
    expect(screen.getByText("–", { selector: ".dispatch-stat strong" })).toBeInTheDocument();
  });
});

describe("report helpers", () => {
  it("rounds the success rate and shows a dash with nothing finished", () => {
    expect(successRate(8, 1)).toBe("89%");
    expect(successRate(0, 0)).toBe("–");
  });

  it("starts a range in Sri Lanka time", () => {
    // 14 Nov 2026 00:30 in Colombo is still 13 Nov in UTC
    const today = new Date("2026-11-13T19:00:00Z");
    expect(rangeStart(1, today)).toBe("2026-11-14");
    expect(rangeStart(7, today)).toBe("2026-11-08");
  });
});
