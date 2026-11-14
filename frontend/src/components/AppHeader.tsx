import { useState } from "react";
import { Link } from "../routes/Link";
import { navigate } from "../routes/router";
import "./AppHeader.css";

export type HeaderNavItem = { label: string; href: string; active?: boolean };
export type HeaderPortal = { label: string; href: string };

type AppHeaderProps = {
  /** The portal being shown, e.g. "Customer portal". */
  portalLabel: string;
  /** Every portal the user can open, including the current one; the switcher only shows when there are two or more. */
  portals: HeaderPortal[];
  currentPortalHref: string;
  nav: HeaderNavItem[];
  user: { fullName: string; email: string };
  onSignOut: () => void;
};

function initials(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0] ?? "?").slice(0, 2);
  return letters.toUpperCase();
}

/** Shared top bar (Figma customer header): brand, the role's pages, portal switcher and the account menu. */
export function AppHeader({ portalLabel, portals, currentPortalHref, nav, user, onSignOut }: AppHeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="app-header">
      <div className="app-header-inner">
        <Link className="app-header-brand" href={currentPortalHref}>
          <span className="app-header-mark" aria-hidden="true">
            S
          </span>
          <span>SwiftDrop</span>
        </Link>

        <nav className="app-header-nav" aria-label="Main">
          {nav.map((item) => (
            <Link
              key={item.href}
              className={`app-header-link${item.active ? " is-active" : ""}`}
              href={item.href}
              aria-current={item.active ? "page" : undefined}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="app-header-tools">
          {portals.length > 1 ? (
            <select
              className="app-header-portal"
              aria-label="Switch portal"
              value={currentPortalHref}
              onChange={(event) => navigate(event.target.value)}
            >
              {portals.map((portal) => (
                <option key={portal.href} value={portal.href}>
                  {portal.label}
                </option>
              ))}
            </select>
          ) : (
            <span className="app-header-portal">{portalLabel}</span>
          )}

          <div className="app-header-account">
            <button
              type="button"
              className="app-header-avatar"
              aria-label={`Account menu for ${user.fullName}`}
              aria-expanded={menuOpen}
              aria-haspopup="menu"
              onClick={() => setMenuOpen((open) => !open)}
              onKeyDown={(event) => event.key === "Escape" && setMenuOpen(false)}
            >
              {initials(user.fullName)}
            </button>
            {menuOpen && (
              <div className="app-header-menu" role="menu">
                <p className="app-header-menu-name">{user.fullName}</p>
                <p className="app-header-menu-email">{user.email}</p>
                <button
                  type="button"
                  role="menuitem"
                  className="app-header-signout"
                  onClick={() => {
                    setMenuOpen(false);
                    onSignOut();
                  }}
                >
                  Sign out
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
