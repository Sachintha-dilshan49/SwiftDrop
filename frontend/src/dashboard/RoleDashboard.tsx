import { greeting } from "../lib/greeting";
import type { ReactNode } from "react";
import { AuthUser } from "../auth/authApi";
import { DashboardRole, dashboardPathForRoles } from "../auth/authPortals";
import { AppHeader } from "../components/AppHeader";

type DashboardContent = {
  label: string;
  heading: string;
  description: string;
};

const dashboardContent: Record<DashboardRole, DashboardContent> = {
  CUSTOMER: {
    label: "Customer portal",
    heading: "My deliveries",
    description: "Track your parcels and manage your recent bookings.",
  },
  MERCHANT: {
    label: "Merchant portal",
    heading: "Business overview",
    description: "Keep an eye on your orders and delivery performance.",
  },
  COURIER: {
    label: "Courier portal",
    heading: "Today's deliveries",
    description: "Your assigned stops for today. Only parcels assigned to you are shown.",
  },
  DISPATCHER: {
    label: "Dispatch portal",
    heading: "Dispatch overview",
    description: "Monitor today's courier workload and delivery exceptions.",
  },
  ADMIN: {
    label: "Admin portal",
    heading: "Platform overview",
    description: "SwiftDrop operations across Colombo today.",
  },
};

type RoleDashboardProps = {
  role: DashboardRole;
  user: AuthUser;
  onSignOut: () => void;
  /** The role's screen. */
  children: ReactNode;
};

export function RoleDashboard({ role, user, onSignOut, children }: RoleDashboardProps) {
  const content = dashboardContent[role];
  const portalPath = (portalRole: DashboardRole) => dashboardPathForRoles([portalRole]) ?? "/";
  const portals = (Object.keys(dashboardContent) as DashboardRole[])
    .filter((portalRole) => user.roles.includes(portalRole))
    .map((portalRole) => ({ label: dashboardContent[portalRole].label, href: portalPath(portalRole) }));

  return (
    <div className={`dashboard dashboard-${role.toLowerCase()}`}>
      <AppHeader
        portalLabel={content.label}
        portals={portals}
        currentPortalHref={portalPath(role)}
        nav={[{ label: content.heading, href: portalPath(role), active: true }]}
        user={user}
        onSignOut={onSignOut}
      />

      <main className="dashboard-main">
        <div className="dashboard-heading">
          <div>
            <p className="dashboard-eyebrow">{content.label}</p>
            <h1>{greeting()}, {user.fullName.split(/\s+/)[0]}</h1>
            <p>{content.description}</p>
          </div>
        </div>

        {children}
      </main>
    </div>
  );
}
