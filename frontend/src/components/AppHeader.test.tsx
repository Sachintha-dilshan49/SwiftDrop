import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AppHeader } from "./AppHeader";

const user = { fullName: "Nimal Perera", email: "nimal@example.test" };
const customerPortal = { label: "Customer portal", href: "/customer" };

function renderHeader(overrides: Partial<Parameters<typeof AppHeader>[0]> = {}) {
  const onSignOut = vi.fn();
  render(
    <AppHeader
      portalLabel="Customer portal"
      portals={[customerPortal]}
      currentPortalHref="/customer"
      nav={[
        { label: "My deliveries", href: "/customer", active: true },
        { label: "Book delivery", href: "/customer/book" },
      ]}
      user={user}
      onSignOut={onSignOut}
      {...overrides}
    />,
  );
  return { onSignOut };
}

describe("AppHeader", () => {
  it("links the brand to the current portal and marks the active page", () => {
    renderHeader();

    expect(screen.getByRole("link", { name: "SwiftDrop" })).toHaveAttribute("href", "/customer");
    const nav = screen.getByRole("navigation", { name: "Main" });
    expect(nav).toContainElement(screen.getByRole("link", { name: "My deliveries" }));
    expect(screen.getByRole("link", { name: "My deliveries" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "Book delivery" })).not.toHaveAttribute("aria-current");
  });

  it("shows the portal name without a switcher when the user has one portal", () => {
    renderHeader();

    expect(screen.getByRole("banner")).toHaveTextContent("Customer portal");
    expect(screen.queryByRole("combobox", { name: "Switch portal" })).not.toBeInTheDocument();
  });

  it("offers a portal switcher when the user has more than one portal", () => {
    renderHeader({
      portalLabel: "Admin portal",
      currentPortalHref: "/admin",
      portals: [
        { label: "Dispatch portal", href: "/dispatch" },
        { label: "Admin portal", href: "/admin" },
      ],
    });

    const switcher = screen.getByRole("combobox", { name: "Switch portal" });
    expect(switcher).toHaveValue("/admin");
    expect(screen.getByRole("option", { name: "Dispatch portal" })).toBeInTheDocument();
  });

  it("shows the user's initials and signs out from the account menu", () => {
    const { onSignOut } = renderHeader();

    const avatar = screen.getByRole("button", { name: "Account menu for Nimal Perera" });
    expect(avatar).toHaveTextContent("NP");
    expect(avatar).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();

    fireEvent.click(avatar);
    expect(screen.getByRole("menu")).toHaveTextContent("nimal@example.test");
    fireEvent.click(screen.getByRole("menuitem", { name: "Sign out" }));

    expect(onSignOut).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  it("closes the account menu with Escape", () => {
    renderHeader();
    const avatar = screen.getByRole("button", { name: "Account menu for Nimal Perera" });

    fireEvent.click(avatar);
    fireEvent.keyDown(avatar, { key: "Escape" });

    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });
});
