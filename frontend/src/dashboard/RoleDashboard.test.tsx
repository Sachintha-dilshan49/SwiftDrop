import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RoleDashboard } from "./RoleDashboard";

const user = { id: "1", fullName: "Demo Customer", email: "demo@example.test", roles: ["CUSTOMER"] };

describe("RoleDashboard", () => {
  it("shows the sample preview when the role has no screen yet", () => {
    render(<RoleDashboard role="COURIER" user={user} onSignOut={vi.fn()} />);

    expect(screen.getByText("Dashboard preview · sample data")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Dashboard summary" })).toBeInTheDocument();
  });

  it("shows the role's own screen instead of the sample preview", () => {
    render(
      <RoleDashboard role="CUSTOMER" user={user} onSignOut={vi.fn()}>
        <p>Real customer screen</p>
      </RoleDashboard>,
    );

    expect(screen.getByText("Real customer screen")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Good morning, Demo" })).toBeInTheDocument();
    expect(screen.queryByText("Dashboard preview · sample data")).not.toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Dashboard summary" })).not.toBeInTheDocument();
  });
});
