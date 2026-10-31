export const authPortals = [
  { id: "customer", label: "Customer", roles: ["CUSTOMER"] },
  { id: "merchant", label: "Merchant", roles: ["MERCHANT"] },
  { id: "courier", label: "Courier", roles: ["COURIER"] },
  { id: "staff", label: "Staff", roles: ["ADMIN", "DISPATCHER"] },
] as const;

export type AuthPortalId = (typeof authPortals)[number]["id"];
export type DashboardRole = "CUSTOMER" | "MERCHANT" | "COURIER" | "DISPATCHER" | "ADMIN";

const dashboardPaths: Record<DashboardRole, string> = {
  CUSTOMER: "/customer",
  MERCHANT: "/merchant",
  COURIER: "/courier",
  DISPATCHER: "/dispatch",
  ADMIN: "/admin",
};

const defaultRolePriority: DashboardRole[] = [
  "ADMIN",
  "DISPATCHER",
  "COURIER",
  "MERCHANT",
  "CUSTOMER",
];

export function portalLabel(portalId: AuthPortalId): string {
  return authPortals.find((portal) => portal.id === portalId)?.label ?? "Customer";
}

export function userHasPortalRole(portalId: AuthPortalId, roles: string[]): boolean {
  const portal = authPortals.find((item) => item.id === portalId);
  return portal ? portal.roles.some((role) => roles.includes(role)) : false;
}

export function dashboardPathForPortal(portalId: AuthPortalId, roles: string[]): string | null {
  const portal = authPortals.find((item) => item.id === portalId);
  const role = portal?.roles.find((candidate) => roles.includes(candidate));
  return role ? dashboardPaths[role] : null;
}

export function dashboardPathForRoles(roles: string[]): string | null {
  const role = defaultDashboardRole(roles);
  return role ? dashboardPaths[role] : null;
}

export function defaultDashboardRole(roles: string[]): DashboardRole | null {
  return defaultRolePriority.find((candidate) => roles.includes(candidate)) ?? null;
}

export function dashboardRoleForPath(path: string): DashboardRole | null {
  return (Object.entries(dashboardPaths) as [DashboardRole, string][])
    .find(([, dashboardPath]) => dashboardPath === path)?.[0] ?? null;
}
