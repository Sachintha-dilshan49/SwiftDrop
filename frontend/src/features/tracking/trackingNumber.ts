/** Same format parcel-service checks (TrackingNumbers.FORMAT): SD-XXXX-XXXX without 0, 1, I or O. */
export const TRACKING_NO_PATTERN = /^SD-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$/;

export function normalizeTrackingNo(input: string): string {
  return input.trim().toUpperCase();
}

export function isValidTrackingNo(trackingNo: string): boolean {
  return TRACKING_NO_PATTERN.test(trackingNo);
}

/** The page address for a tracking number; emails link to it (TRACKING_BASE_URL + "/" + number). */
export function trackingPath(trackingNo: string): string {
  return `/track/${encodeURIComponent(trackingNo)}`;
}

/**
 * For the public tracking route: "" for /track, the (normalized) tracking number for /track/{number},
 * and null for every other path.
 */
export function trackingNumberFromPath(pathname: string): string | null {
  const match = /^\/track(?:\/([^/]*))?\/?$/.exec(pathname);
  if (!match) {
    return null;
  }
  try {
    return normalizeTrackingNo(decodeURIComponent(match[1] ?? ""));
  } catch {
    return "";
  }
}
