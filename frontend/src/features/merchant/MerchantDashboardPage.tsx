import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantOrderResponse, MerchantResponse } from '../../api/merchantApi';
import type { ParcelStatus } from '../../api/parcelApi';
import { StatusBadge } from '../../components/StatusBadge';
import { ApiKeysPage } from './ApiKeysPage';
import { WebhookSettingsPage } from './WebhookSettingsPage';
import { LIVE_STATUSES as LIVE, orderStats } from './merchantStats';
import './merchant.css';

type TabKey = 'overview' | 'orders' | 'developer';
type Filter = 'ALL' | 'LIVE' | 'DELIVERED' | 'ISSUES';

const TABS: { id: TabKey; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'orders', label: 'Orders' },
  { id: 'developer', label: 'Developer settings' },
];

/** The most recent orders this screen loads. */
const ORDER_LIMIT = 100;
const RECENT_ON_OVERVIEW = 10;

const PARCEL_STATUSES: ParcelStatus[] = ['CREATED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'];

const FILTERS: { id: Filter; label: string; matches: (status: string) => boolean }[] = [
  { id: 'ALL', label: 'All', matches: () => true },
  { id: 'LIVE', label: 'Live', matches: (s) => (LIVE as string[]).includes(s) },
  { id: 'DELIVERED', label: 'Delivered', matches: (s) => s === 'DELIVERED' },
  { id: 'ISSUES', label: 'Failed', matches: (s) => s === 'FAILED' },
];

const SL_TIME = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Colombo', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit', hour12: true,
});

function isParcelStatus(status: string): status is ParcelStatus {
  return (PARCEL_STATUSES as string[]).includes(status);
}

function money(amount: number | null | undefined, currency: string): string {
  return amount == null ? '–' : `${currency} ${amount.toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

interface Props {
  token?: string;
  initialTab?: TabKey;
}

/** The merchant's portal: their orders and how they are doing, and their API keys and webhook. */
export const MerchantDashboardPage: React.FC<Props> = ({ token, initialTab = 'overview' }) => {
  const [tab, setTab] = useState<TabKey>(initialTab);
  const [profile, setProfile] = useState<MerchantResponse | null>(null);
  const [orders, setOrders] = useState<MerchantOrderResponse[] | null>(null);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    if (!token) return;
    let current = true;
    Promise.all([merchantApi.getMyProfile(token), merchantApi.listOrders(token, 0, ORDER_LIMIT)])
      .then(([me, page]) => {
        if (!current) return;
        setProfile(me);
        setOrders(page?.content ?? []);
        setTotal(page?.totalElements ?? page?.content?.length ?? 0);
      })
      .catch((err: unknown) => {
        if (current) setError(err instanceof Error ? err.message : 'Could not load your orders. Please try again.');
      });
    return () => {
      current = false;
    };
  }, [token]);

  const stats = orders ? orderStats(orders) : null;
  const matching = (orders ?? []).filter((o) => FILTERS.find((f) => f.id === filter)!.matches(o.status));
  const query = search.trim().toLowerCase();
  const searched = matching.filter(
    (o) =>
      !query ||
      o.trackingNo.toLowerCase().includes(query) ||
      o.recipientName.toLowerCase().includes(query) ||
      o.dropoffAddress.toLowerCase().includes(query)
  );

  return (
    <div className="merchant-portal">
      {profile && (
        <p className="merchant-business">
          <strong>{profile.businessName}</strong>
          <span>{profile.status === 'APPROVED' ? 'Approved merchant' : profile.status.toLowerCase()}</span>
        </p>
      )}

      <div className="merchant-tabs" role="tablist" aria-label="Merchant portal">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            id={`merchant-tab-${item.id}`}
            aria-selected={tab === item.id}
            aria-controls={`merchant-panel-${item.id}`}
            className="merchant-tab"
            onClick={() => setTab(item.id)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`merchant-panel-${tab}`} aria-labelledby={`merchant-tab-${tab}`}>
        {error && tab !== 'developer' && <div className="alert" role="alert">{error}</div>}

        {tab === 'overview' && (
          <>
            <section className="merchant-stats-grid" aria-label="Order summary">
              <Stat label="Orders today" value={stats?.today} sub={stats ? `${stats.todayWaiting} waiting for pickup` : ''} />
              <Stat label="Live right now" value={stats?.live} sub="Not yet delivered" />
              <Stat label="Delivered" value={stats?.deliveredWeek} sub="Last 7 days" />
              <Stat label="Failed" value={stats?.failedWeek} sub="Last 7 days" />
            </section>
            <OrderList
              title="Recent orders"
              orders={matching.slice(0, RECENT_ON_OVERVIEW)}
              all={orders}
              filter={filter}
              onFilter={setFilter}
              loading={!orders && !error}
            />
          </>
        )}

        {tab === 'orders' && (
          <>
            <div className="merchant-search-wrapper merchant-orders-search">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="search"
                aria-label="Search orders"
                placeholder="Search by tracking number, recipient or address…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <OrderList
              title={total > ORDER_LIMIT ? `Your latest ${ORDER_LIMIT} of ${total} orders` : 'All orders'}
              orders={searched}
              all={orders}
              filter={filter}
              onFilter={setFilter}
              loading={!orders && !error}
            />
          </>
        )}

        {tab === 'developer' && token && (
          <div className="merchant-developer">
            <section className="dev-card">
              <h2 className="dev-card-title">Create orders</h2>
              <p className="merchant-muted">
                Your store creates orders through the Merchant API with one of your API keys. Each order books a
                SwiftDrop delivery and appears here with its tracking number. Zone codes such as{' '}
                <code>COLOMBO_SOUTH</code> or <code>KANDY</code> are listed at <code>GET /api/v1/pricing/zones</code>.
              </p>
              <pre className="merchant-code">{`curl -X POST ${window.location.origin}/api/v1/merchant-api/orders \\
  -H "X-API-Key: <your API key>" \\
  -H "Content-Type: application/json" \\
  -d '{"recipientName": "Nimal Perera", "recipientPhone": "0771234567",
       "recipientEmail": "nimal@example.com",
       "pickupAddress": "${profile?.address ?? '12 Galle Road, Colombo 03'}",
       "pickupZone": "<your pickup zone code>", "dropoffAddress": "5 Station Road, Nugegoda",
       "dropoffZone": "NUGEGODA_MAHARAGAMA", "weightKg": 1.5, "size": "SMALL",
       "speed": "STANDARD", "idempotencyKey": "order-1001"}'`}</pre>
            </section>
            <ApiKeysPage token={token} />
            <WebhookSettingsPage token={token} />
          </div>
        )}
      </div>
    </div>
  );
};

function Stat({ label, value, sub }: { label: string; value: number | undefined; sub: string }) {
  return (
    <div className="merchant-stat-card">
      <span className="merchant-stat-label">{label}</span>
      <strong className="merchant-stat-value">{value ?? '–'}</strong>
      <span className="merchant-stat-sub">{sub}</span>
    </div>
  );
}

function OrderList(props: {
  title: string;
  orders: MerchantOrderResponse[];
  all: MerchantOrderResponse[] | null;
  filter: Filter;
  onFilter: (filter: Filter) => void;
  loading: boolean;
}) {
  const { title, orders, all, filter, onFilter, loading } = props;
  return (
    <section aria-label={title}>
      <div className="merchant-filters-row">
        <h2 className="merchant-list-title">{title}</h2>
        <div className="merchant-filter-pills" role="group" aria-label="Filter orders">
          {FILTERS.map((f) => (
            <button
              key={f.id}
              type="button"
              aria-pressed={filter === f.id}
              className={`merchant-filter-pill ${filter === f.id ? 'is-active' : ''}`}
              onClick={() => onFilter(f.id)}
            >
              {f.label} {all ? all.filter((o) => f.matches(o.status)).length : ''}
            </button>
          ))}
        </div>
      </div>

      <div className="merchant-card-box">
        <table className="merchant-table">
          <thead>
            <tr>
              <th>Tracking no</th>
              <th>Recipient</th>
              <th>Destination</th>
              <th>Fee</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={6} className="merchant-muted">Loading orders…</td></tr>
            )}
            {!loading && orders.length === 0 && (
              <tr>
                <td colSpan={6} className="merchant-muted">
                  {all && all.length === 0
                    ? 'No orders yet. Create one through the Merchant API (see Developer settings).'
                    : 'No orders match.'}
                </td>
              </tr>
            )}
            {orders.map((o) => (
              <tr key={o.id}>
                <td><code className="merchant-tracking">{o.trackingNo}</code></td>
                <td><strong>{o.recipientName}</strong><br /><span className="merchant-muted">{o.recipientPhone}</span></td>
                <td>{o.dropoffAddress}</td>
                <td>{money(o.feeAmount, o.feeCurrency)}</td>
                <td>{isParcelStatus(o.status) ? <StatusBadge status={o.status} /> : o.status}</td>
                <td className="merchant-muted">{SL_TIME.format(new Date(o.createdAt))}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
