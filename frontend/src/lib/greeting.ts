/** "Good morning/afternoon/evening" by the time in Sri Lanka, where SwiftDrop works. */
export function greeting(now: Date = new Date()): string {
  const hour = Number(new Intl.DateTimeFormat("en-GB", { timeZone: "Asia/Colombo", hour: "numeric", hourCycle: "h23" }).format(now));
  return hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
}
