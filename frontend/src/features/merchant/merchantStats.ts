import type { MerchantOrderResponse } from '../../api/merchantApi';
import type { ParcelStatus } from '../../api/parcelApi';

/** Statuses of an order that is not finished yet. */
export const LIVE_STATUSES: ParcelStatus[] = ['CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY'];
const LIVE = LIVE_STATUSES;

/** Sri Lanka calendar day, e.g. 2026-11-14. */
const SL_DAY = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Colombo' });

/** Counts for the summary cards, from the loaded orders. */
export function orderStats(orders: MerchantOrderResponse[], now: Date = new Date()) {
  const today = SL_DAY.format(now);
  const weekAgo = now.getTime() - 7 * 86_400_000;
  const createdToday = orders.filter((o) => SL_DAY.format(new Date(o.createdAt)) === today);
  const lastWeek = orders.filter((o) => new Date(o.createdAt).getTime() >= weekAgo);
  return {
    today: createdToday.length,
    todayWaiting: createdToday.filter((o) => o.status === 'CREATED').length,
    live: orders.filter((o) => (LIVE as string[]).includes(o.status)).length,
    deliveredWeek: lastWeek.filter((o) => o.status === 'DELIVERED').length,
    failedWeek: lastWeek.filter((o) => o.status === 'FAILED').length,
  };
}

