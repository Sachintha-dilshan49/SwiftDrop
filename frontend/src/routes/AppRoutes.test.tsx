import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "./AppRoutes";

vi.mock("../auth/AuthApp", () => ({ AuthApp: () => <p>auth app</p> }));
vi.mock("../features/tracking/PublicTrackingPage", () => ({
  PublicTrackingPage: ({ initialTrackingNo }: { initialTrackingNo: string }) => <p>tracking {initialTrackingNo}</p>,
}));

describe("AppRoutes", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "/");
  });

  it("opens public tracking without a login", () => {
    window.history.replaceState(null, "", "/track/SD-7K3Q-9P2M");
    render(<AppRoutes />);

    expect(screen.getByText("tracking SD-7K3Q-9P2M")).toBeInTheDocument();
  });

  it("hands every other path to AuthApp", () => {
    window.history.replaceState(null, "", "/customer");
    render(<AppRoutes />);

    expect(screen.getByText("auth app")).toBeInTheDocument();
  });
});
