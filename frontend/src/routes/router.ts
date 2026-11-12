// The app's only router (ADR-012): the browser history API plus a hook, no routing library.
import { useSyncExternalStore } from "react";

// pushState and replaceState do not fire popstate, so navigate() announces its own changes.
const NAVIGATE_EVENT = "swiftdrop:navigate";

function subscribe(onChange: () => void): () => void {
  window.addEventListener("popstate", onChange);
  window.addEventListener(NAVIGATE_EVENT, onChange);
  return () => {
    window.removeEventListener("popstate", onChange);
    window.removeEventListener(NAVIGATE_EVENT, onChange);
  };
}

function currentPath(): string {
  return window.location.pathname;
}

/** Opens a path inside the app; with replace, the current history entry is replaced (no new Back step). */
export function navigate(path: string, options: { replace?: boolean } = {}): void {
  if (options.replace) {
    window.history.replaceState(null, "", path);
  } else {
    window.history.pushState(null, "", path);
  }
  window.dispatchEvent(new Event(NAVIGATE_EVENT));
}

/** The current path; re-renders on navigate() and on the browser's Back and Forward buttons. */
export function usePath(): string {
  return useSyncExternalStore(subscribe, currentPath);
}
