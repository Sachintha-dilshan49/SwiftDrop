import { DashboardRole, dashboardRoleForPath } from "../auth/authPortals";
import { trackingNumberFromPath } from "../features/tracking/trackingNumber";

export type Route =
  | { kind: "tracking"; trackingNo: string } // public: /track and /track/{trackingNo}
  | { kind: "callback" } // Google sign-in returns here
  | { kind: "login" }
  | { kind: "home" } // signed in, no portal chosen yet: go to the default dashboard
  | { kind: "dashboard"; role: DashboardRole }
  | { kind: "forbidden" } // 403: another role's portal
  | { kind: "notFound" }; // 404

/**
 * What a path shows. roles is null when nobody is signed in. Role checks here only choose the screen;
 * every service still checks the token itself (AGENTS.md §5.9).
 */
export function resolveRoute(path: string, roles: string[] | null): Route {
  const trackingNo = trackingNumberFromPath(path);
  if (trackingNo !== null) {
    return { kind: "tracking", trackingNo };
  }

  const cleanPath = path.length > 1 ? path.replace(/\/+$/, "") : path;
  if (cleanPath === "/auth/callback") {
    return roles ? { kind: "home" } : { kind: "callback" };
  }
  if (cleanPath === "/") {
    return roles ? { kind: "home" } : { kind: "login" };
  }

  const role = dashboardRoleForPath(cleanPath);
  if (role) {
    if (!roles) {
      return { kind: "login" };
    }
    return roles.includes(role) ? { kind: "dashboard", role } : { kind: "forbidden" };
  }
  return { kind: "notFound" };
}
