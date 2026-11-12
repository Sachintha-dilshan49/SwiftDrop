import { AuthApp } from "../auth/AuthApp";
import { PublicTrackingPage } from "../features/tracking/PublicTrackingPage";
import { resolveRoute } from "./routes";
import { usePath } from "./router";

/** Top of the app: the public tracking page needs no login; AuthApp handles every other path. */
export function AppRoutes() {
  const route = resolveRoute(usePath(), null);
  return route.kind === "tracking" ? <PublicTrackingPage initialTrackingNo={route.trackingNo} /> : <AuthApp />;
}
