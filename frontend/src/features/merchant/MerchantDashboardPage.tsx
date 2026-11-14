import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantOrderResponse } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  token?: string;
  onSignOut?: () => void;
  onSwitchPortal?: (portal: 'customer' | 'merchant' | 'admin') => void;
  initialTab?: 'overview' | 'orders' | 'developer';
  embedded?: boolean;
}

type TabKey = 'overview' | 'orders' | 'developer';

export const MerchantDashboardPage: React.FC<Props> = ({
  token,
  onSignOut,
  onSwitchPortal,
  initialTab = 'overview',
  embedded = false,
}) => {
  const [activeTab, setActiveTab] = useState<TabKey>(initialTab);
  const [orders, setOrders] = useState<MerchantOrderResponse[]>([]);
  const [loading, setLoading] = useState(Boolean(token));
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isSandbox, setIsSandbox] = useState(false);
  const [showLiveKey, setShowLiveKey] = useState(false);
  const [showTestKey, setShowTestKey] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [copiedCurl, setCopiedCurl] = useState(false);
  const [webhookUrl, setWebhookUrl] = useState('https://api.lotusleaf.lk/webhooks/swiftdrop');
  const [webhookActive, setWebhookActive] = useState(true);
  const [testSent, setTestSent] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // New Order Form State
  const [newOrder, setNewOrder] = useState({
    recipientName: '',
    recipientPhone: '',
    recipientEmail: '',
    dropoffAddress: '',
    dropoffZone: 'COLOMBO_SOUTH',
    weightKg: 1.5,
    speed: 'STANDARD',
  });
  const [creatingOrder, setCreatingOrder] = useState(false);

  // Demo fallback orders matching Figma 1:1754 and 1:2129
  const defaultOrders: MerchantOrderResponse[] = [
    {
      id: 'ord-1',
      parcelId: 'p-1',
      trackingNo: 'SD-20479',
      recipientName: 'Hashini De Silva',
      recipientPhone: '+94 77 123 4567',
      dropoffAddress: 'Borella Junction, Colombo 08',
      status: 'AWAITING_COURIER',
      feeAmount: 450.0,
      feeCurrency: 'LKR',
      createdAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
    },
    {
      id: 'ord-2',
      parcelId: 'p-2',
      trackingNo: 'SD-20478',
      recipientName: 'Malith Gunasekara',
      recipientPhone: '+94 71 987 6543',
      dropoffAddress: 'Nugegoda Junction, Colombo',
      status: 'AWAITING_COURIER',
      feeAmount: 520.0,
      feeCurrency: 'LKR',
      createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(),
    },
    {
      id: 'ord-3',
      parcelId: 'p-3',
      trackingNo: 'SD-20476',
      recipientName: 'Sachini Herath',
      recipientPhone: '+94 76 345 6789',
      dropoffAddress: 'Liberty Plaza, Colombo 03',
      status: 'IN_TRANSIT',
      feeAmount: 380.0,
      feeCurrency: 'LKR',
      createdAt: new Date(Date.now() - 1000 * 60 * 80).toISOString(),
    },
    {
      id: 'ord-4',
      parcelId: 'p-4',
      trackingNo: 'SD-20465',
      recipientName: 'Dilan Perera',
      recipientPhone: '+94 72 456 7890',
      dropoffAddress: 'No. 15, Havelock Road, Colombo 05',
      status: 'IN_TRANSIT',
      feeAmount: 410.0,
      feeCurrency: 'LKR',
      createdAt: new Date(Date.now() - 1000 * 60 * 140).toISOString(),
    },
    {
      id: 'ord-5',
      parcelId: 'p-5',
      trackingNo: 'SD-20455',
      recipientName: 'Kavindi Fernando',
      recipientPhone: '+94 77 654 3210',
      dropoffAddress: 'Trace Expert City, Maradana',
      status: 'DELIVERED',
      feeAmount: 490.0,
      feeCurrency: 'LKR',
      createdAt: new Date(Date.now() - 1000 * 60 * 300).toISOString(),
    },
    {
      id: 'ord-6',
      parcelId: 'p-6',
      trackingNo: 'SD-20442',
      recipientName: 'Roshan Silva',
      recipientPhone: '+94 70 112 2334',
      dropoffAddress: '12 Temple Road, Galle',
      status: 'FAILED',
      feeAmount: 850.0,
      feeCurrency: 'LKR',
      createdAt: new Date(Date.now() - 1000 * 60 * 500).toISOString(),
    },
  ];

  const fetchOrders = async () => {
    if (!token) {
      setOrders(defaultOrders);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const res = await merchantApi.listOrders(token, 0, 30);
      if (res && res.content && res.content.length > 0) {
        setOrders(res.content);
      } else {
        setOrders(defaultOrders);
      }
    } catch {
      setOrders(defaultOrders);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [token]);

  const copyToClipboard = (text: string, type: string) => {
    navigator.clipboard.writeText(text);
    if (type === 'curl') {
      setCopiedCurl(true);
      setTimeout(() => setCopiedCurl(false), 2000);
    } else {
      setCopiedKey(type);
      setTimeout(() => setCopiedKey(null), 2000);
    }
  };

  const handleCreateOrder = (e: React.FormEvent) => {
    e.preventDefault();
    setCreatingOrder(true);
    setTimeout(() => {
      const generatedTracking = `SD-${Math.floor(20480 + Math.random() * 900)}`;
      const newEntry: MerchantOrderResponse = {
        id: `ord-${Date.now()}`,
        parcelId: `p-${Date.now()}`,
        trackingNo: generatedTracking,
        recipientName: newOrder.recipientName || 'Sample Recipient',
        recipientPhone: newOrder.recipientPhone || '+94 77 123 4567',
        dropoffAddress: newOrder.dropoffAddress || 'Colombo 03',
        status: 'AWAITING_COURIER',
        feeAmount: 480.0,
        feeCurrency: 'LKR',
        createdAt: new Date().toISOString(),
      };
      setOrders([newEntry, ...orders]);
      setCreatingOrder(false);
      setShowCreateModal(false);
      setNewOrder({
        recipientName: '',
        recipientPhone: '',
        recipientEmail: '',
        dropoffAddress: '',
        dropoffZone: 'COLOMBO_SOUTH',
        weightKg: 1.5,
        speed: 'STANDARD',
      });
    }, 600);
  };

  const filteredOrders = orders.filter((o) => {
    const matchesSearch =
      o.trackingNo.toLowerCase().includes(searchQuery.toLowerCase()) ||
      o.recipientName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      o.dropoffAddress.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;
    if (filter === 'ALL') return true;
    if (filter === 'LIVE') return o.status === 'AWAITING_COURIER' || o.status === 'IN_TRANSIT';
    if (filter === 'DELIVERED') return o.status === 'DELIVERED';
    if (filter === 'ISSUES') return o.status === 'FAILED';
    return true;
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'AWAITING_COURIER':
        return <span className="status-pill awaiting">● Awaiting courier</span>;
      case 'IN_TRANSIT':
      case 'PICKED_UP':
        return <span className="status-pill transit">● In transit</span>;
      case 'DELIVERED':
        return <span className="status-pill delivered">● Delivered</span>;
      case 'FAILED':
        return <span className="status-pill failed">● Failed</span>;
      default:
        return <span className="status-pill awaiting">{status}</span>;
    }
  };

  const curlSnippet = `curl -X POST https://api.swiftdrop.lk/api/v1/merchants/me/orders \\
  -H "X-API-Key: ${isSandbox ? 'sd_test_9d42f0b784a1d6f0' : 'sd_live_4a81b9e236c54f2a'}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "recipientName": "Sunil Fernando",
    "recipientPhone": "+94771234567",
    "dropoffAddress": "12 Temple Road, Galle",
    "dropoffZone": "INTER_CITY",
    "weightKg": 1.5,
    "speed": "STANDARD"
  }'`;

  return (
    <div className="merchant-layout">
      {/* Sidebar Navigation matching Figma frames 1:1754, 1:2129, 1:2731 */}
      <aside className="merchant-sidebar">
        <div>
          <div className="merchant-sidebar-brand">
            <div className="brand-mark">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
              </svg>
            </div>
            <span className="brand-title">SwiftDrop</span>
            <span className="merchant-role-tag">Merchant</span>
          </div>

          <nav>
            <ul className="merchant-nav">
              <li>
                <button
                  type="button"
                  className={`merchant-nav-item ${activeTab === 'overview' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('overview')}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="3" y="3" width="7" height="7" />
                    <rect x="14" y="3" width="7" height="7" />
                    <rect x="14" y="14" width="7" height="7" />
                    <rect x="3" y="14" width="7" height="7" />
                  </svg>
                  Overview
                </button>
              </li>
              <li>
                <button
                  type="button"
                  className={`merchant-nav-item ${activeTab === 'orders' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('orders')}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                    <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                    <line x1="12" y1="22.08" x2="12" y2="12" />
                  </svg>
                  Orders
                </button>
              </li>
              <li>
                <button
                  type="button"
                  className={`merchant-nav-item ${activeTab === 'developer' ? 'is-active' : ''}`}
                  onClick={() => setActiveTab('developer')}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="16 18 22 12 16 6" />
                    <polyline points="8 6 2 12 8 18" />
                  </svg>
                  Developer settings
                </button>
              </li>
            </ul>
          </nav>
        </div>

        <div className="merchant-sidebar-footer">
          {/* Demo Portal Switcher */}
          <div className="merchant-portal-switcher">
            <span className="merchant-portal-switcher-label">Switch portal</span>
            <div className="merchant-switcher-buttons">
              <button
                type="button"
                className="merchant-switcher-btn"
                onClick={() => onSwitchPortal && onSwitchPortal('customer')}
              >
                Customer
              </button>
              <button type="button" className="merchant-switcher-btn is-active">
                Merchant
              </button>
              <button
                type="button"
                className="merchant-switcher-btn"
                onClick={() => onSwitchPortal && onSwitchPortal('admin')}
              >
                Admin
              </button>
            </div>
          </div>

          {/* User profile card */}
          <div className="merchant-user-profile">
            <div className="merchant-user-info">
              <div className="merchant-avatar">AR</div>
              <div className="merchant-user-text">
                <h4>Ayesha Rodrigo</h4>
                <p>Lotus Leaf Bakery</p>
              </div>
            </div>
            {onSignOut && (
              <button
                type="button"
                className="merchant-signout-btn"
                onClick={onSignOut}
                title="Sign out"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
              </button>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="merchant-content-area">
        {/* Top Header */}
        {embedded ? (
          <div className="merchant-topbar">
            <div className="merchant-search-wrapper">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                placeholder="Search orders, riders, merchants…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="merchant-topbar-actions">
              <span className="merchant-portal-pill">Merchant portal</span>
              <button type="button" className="merchant-icon-btn" aria-label="Notifications">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
                <span className="merchant-bell-dot" />
              </button>
              <div className="merchant-avatar" style={{ width: '28px', height: '28px', fontSize: '10px' }}>
                AR
              </div>
            </div>
          </div>
        ) : (
          <header className="merchant-topbar">
            <div className="merchant-search-wrapper">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                placeholder="Search orders, riders, merchants…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className="merchant-topbar-actions">
              <span className="merchant-portal-pill">Merchant portal</span>
              <button type="button" className="merchant-icon-btn" aria-label="Notifications">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
                <span className="merchant-bell-dot" />
              </button>
              <div className="merchant-avatar" style={{ width: '28px', height: '28px', fontSize: '10px' }}>
                AR
              </div>
            </div>
          </header>
        )}

        {/* Body Pages */}
        <main className="merchant-body">
          {error && <div className="alert" role="alert">{error}</div>}

          {/* TAB 1: OVERVIEW (Figma 1:1754) */}
          {activeTab === 'overview' && (
            <div>
              <div className="merchant-page-head">
                <div>
                  <h1>Business overview</h1>
                  <p>Keep an eye on your orders and delivery performance.</p>
                </div>
                <button
                  type="button"
                  className="button button-primary"
                  style={{ width: 'auto', padding: '0 20px', height: '40px' }}
                  onClick={() => setShowCreateModal(true)}
                >
                  + Create order
                </button>
              </div>

              {/* 4 Stat Cards */}
              <section className="merchant-stats-grid" aria-label="Merchant summary metrics">
                <div className="merchant-stat-card">
                  <span className="merchant-stat-label">Orders today</span>
                  <strong className="merchant-stat-value">5</strong>
                  <span className="merchant-stat-sub">2 awaiting a courier</span>
                </div>
                <div className="merchant-stat-card">
                  <span className="merchant-stat-label">Live right now</span>
                  <strong className="merchant-stat-value">3</strong>
                  <span className="merchant-stat-sub">In progress</span>
                </div>
                <div className="merchant-stat-card">
                  <span className="merchant-stat-label">Delivered this week</span>
                  <strong className="merchant-stat-value">142</strong>
                  <span className="merchant-stat-sub">98.5% on time</span>
                </div>
                <div className="merchant-stat-card">
                  <span className="merchant-stat-label">COD collected</span>
                  <strong className="merchant-stat-value">Rs 4,200</strong>
                  <span className="merchant-stat-sub">Settles Friday</span>
                </div>
              </section>

              {/* Orders Table with Filter Pills */}
              <div className="merchant-filters-row">
                <div className="merchant-filter-pills" role="tablist">
                  <button
                    type="button"
                    className={`merchant-filter-pill ${filter === 'ALL' ? 'is-active' : ''}`}
                    onClick={() => setFilter('ALL')}
                  >
                    All 6
                  </button>
                  <button
                    type="button"
                    className={`merchant-filter-pill ${filter === 'LIVE' ? 'is-active' : ''}`}
                    onClick={() => setFilter('LIVE')}
                  >
                    Live 3
                  </button>
                  <button
                    type="button"
                    className={`merchant-filter-pill ${filter === 'DELIVERED' ? 'is-active' : ''}`}
                    onClick={() => setFilter('DELIVERED')}
                  >
                    Delivered 2
                  </button>
                  <button
                    type="button"
                    className={`merchant-filter-pill ${filter === 'ISSUES' ? 'is-active' : ''}`}
                    onClick={() => setFilter('ISSUES')}
                  >
                    Issues 1
                  </button>
                </div>
              </div>

              <div className="merchant-card-box">
                <table className="merchant-table">
                  <thead>
                    <tr>
                      <th>Tracking No</th>
                      <th>Recipient</th>
                      <th>Destination</th>
                      <th>Fee / COD</th>
                      <th>Status</th>
                      <th>Created</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading ? (
                      <tr>
                        <td colSpan={6} style={{ textAlign: 'center', padding: '30px' }}>
                          Loading orders…
                        </td>
                      </tr>
                    ) : filteredOrders.map((ord) => (
                      <tr key={ord.id}>
                        <td>
                          <span className="merchant-tracking-link">{ord.trackingNo}</span>
                        </td>
                        <td>
                          <div style={{ fontWeight: 700 }}>{ord.recipientName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>{ord.recipientPhone}</div>
                        </td>
                        <td style={{ color: '#4d5b70' }}>{ord.dropoffAddress}</td>
                        <td>
                          <strong>{ord.feeCurrency} {Number(ord.feeAmount).toFixed(2)}</strong>
                        </td>
                        <td>{getStatusBadge(ord.status)}</td>
                        <td style={{ fontSize: '11px', color: 'var(--muted)' }}>
                          {new Date(ord.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 2: ORDERS (Figma 1:2129) */}
          {activeTab === 'orders' && (
            <div>
              <div className="merchant-page-head">
                <div>
                  <h1>Orders list</h1>
                  <p>All parcels created by dashboard or automated API.</p>
                </div>
                <button
                  type="button"
                  className="button button-primary"
                  style={{ width: 'auto', padding: '0 20px', height: '40px' }}
                  onClick={() => setShowCreateModal(true)}
                >
                  + Create order
                </button>
              </div>

              <div className="merchant-filters-row">
                <div className="merchant-filter-pills">
                  {['ALL', 'LIVE', 'DELIVERED', 'ISSUES'].map((f) => (
                    <button
                      key={f}
                      type="button"
                      className={`merchant-filter-pill ${filter === f ? 'is-active' : ''}`}
                      onClick={() => setFilter(f)}
                    >
                      {f === 'ALL' ? 'All orders' : f.charAt(0) + f.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </div>

              <div className="merchant-card-box">
                <table className="merchant-table">
                  <thead>
                    <tr>
                      <th>Tracking No</th>
                      <th>Recipient</th>
                      <th>Destination</th>
                      <th>Fee</th>
                      <th>Status</th>
                      <th>Created</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredOrders.map((ord) => (
                      <tr key={ord.id}>
                        <td>
                          <span className="merchant-tracking-link">{ord.trackingNo}</span>
                        </td>
                        <td>
                          <div style={{ fontWeight: 700 }}>{ord.recipientName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>{ord.recipientPhone}</div>
                        </td>
                        <td>{ord.dropoffAddress}</td>
                        <td>
                          <strong>{ord.feeCurrency} {Number(ord.feeAmount).toFixed(2)}</strong>
                        </td>
                        <td>{getStatusBadge(ord.status)}</td>
                        <td style={{ fontSize: '11px', color: 'var(--muted)' }}>
                          {new Date(ord.createdAt).toLocaleDateString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 3: DEVELOPER SETTINGS (Figma 1:2731) */}
          {activeTab === 'developer' && (
            <div>
              <div className="merchant-page-head">
                <div>
                  <h1>Developer settings</h1>
                  <p>API keys, webhook endpoints and integration credentials.</p>
                </div>

                {/* Sandbox Switch */}
                <div className="sandbox-mode-switch">
                  <span>Sandbox mode</span>
                  <div
                    className={`switch-pill ${isSandbox ? 'is-active' : ''}`}
                    onClick={() => setIsSandbox(!isSandbox)}
                    role="switch"
                    aria-checked={isSandbox}
                  >
                    <div className="switch-pill-handle" />
                  </div>
                </div>
              </div>

              {/* Two Column Layout matching Figma 1:2731 */}
              <div className="dev-settings-grid">
                {/* Left Column: API Keys & Webhooks */}
                <div className="dev-col">
                  {/* API Keys Card */}
                  <div className="dev-card">
                    <div className="dev-card-head">
                      <h2 className="dev-card-title">API keys</h2>
                      <span className="merchant-role-tag">REST API</span>
                    </div>
                    <p className="dev-card-desc">
                      Authenticate external e-commerce systems with the <code>X-API-Key</code> header. Keep production keys secret.
                    </p>

                    {/* Live Key */}
                    <div className="dev-key-row">
                      <label className="dev-key-label">Production key (Live)</label>
                      <div className="dev-key-input-box">
                        <input
                          type={showLiveKey ? 'text' : 'password'}
                          readOnly
                          value="sd_live_4a81b9e236c54f2a"
                        />
                        <button
                          type="button"
                          className="dev-action-btn-icon"
                          onClick={() => setShowLiveKey(!showLiveKey)}
                          title="Show / Hide"
                        >
                          👁
                        </button>
                        <button
                          type="button"
                          className="dev-action-btn-icon"
                          onClick={() => copyToClipboard('sd_live_4a81b9e236c54f2a', 'live')}
                          title="Copy key"
                        >
                          {copiedKey === 'live' ? '✓' : '📋'}
                        </button>
                      </div>
                    </div>

                    {/* Test Key */}
                    <div className="dev-key-row">
                      <label className="dev-key-label">Test key (Sandbox)</label>
                      <div className="dev-key-input-box">
                        <input
                          type={showTestKey ? 'text' : 'password'}
                          readOnly
                          value="sd_test_9d42f0b784a1d6f0"
                        />
                        <button
                          type="button"
                          className="dev-action-btn-icon"
                          onClick={() => setShowTestKey(!showTestKey)}
                          title="Show / Hide"
                        >
                          👁
                        </button>
                        <button
                          type="button"
                          className="dev-action-btn-icon"
                          onClick={() => copyToClipboard('sd_test_9d42f0b784a1d6f0', 'test')}
                          title="Copy key"
                        >
                          {copiedKey === 'test' ? '✓' : '📋'}
                        </button>
                      </div>
                    </div>

                    <button
                      type="button"
                      className="button button-outline"
                      style={{ width: 'auto', padding: '0 18px', height: '36px', fontSize: '11px', marginTop: '10px' }}
                      onClick={() => alert('New API key generated!')}
                    >
                      Regenerate live key
                    </button>
                  </div>

                  {/* Webhooks Card */}
                  <div className="dev-card">
                    <div className="dev-card-head">
                      <h2 className="dev-card-title">Webhooks</h2>
                      <div
                        className={`switch-pill ${webhookActive ? 'is-active' : ''}`}
                        onClick={() => setWebhookActive(!webhookActive)}
                        role="switch"
                        aria-checked={webhookActive}
                        title="Enable / Disable Webhook"
                      >
                        <div className="switch-pill-handle" />
                      </div>
                    </div>
                    <p className="dev-card-desc">
                      SwiftDrop sends real-time POST requests when delivery statuses change.
                    </p>

                    <div className="reg-form-group">
                      <label htmlFor="webhookUrl">Webhook Endpoint URL</label>
                      <input
                        id="webhookUrl"
                        type="url"
                        value={webhookUrl}
                        onChange={(e) => setWebhookUrl(e.target.value)}
                        placeholder="https://your-domain.com/webhook"
                      />
                    </div>

                    <div className="dev-key-row">
                      <label className="dev-key-label">Signing Secret (HMAC-SHA256)</label>
                      <div className="dev-key-input-box">
                        <input
                          type="password"
                          readOnly
                          value="whsec_98f4b23d90184e56"
                        />
                        <button
                          type="button"
                          className="dev-action-btn-icon"
                          onClick={() => copyToClipboard('whsec_98f4b23d90184e56', 'whsec')}
                          title="Copy signing secret"
                        >
                          {copiedKey === 'whsec' ? '✓' : '📋'}
                        </button>
                      </div>
                    </div>

                    <div style={{ marginTop: '16px' }}>
                      <span className="dev-key-label">Subscribed Events</span>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '11px' }}>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <input type="checkbox" defaultChecked /> parcel.created
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <input type="checkbox" defaultChecked /> parcel.picked_up
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <input type="checkbox" defaultChecked /> parcel.in_transit
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <input type="checkbox" defaultChecked /> parcel.delivered
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <input type="checkbox" defaultChecked /> parcel.failed
                        </label>
                      </div>
                    </div>

                    <button
                      type="button"
                      className="button button-primary"
                      style={{ width: 'auto', padding: '0 20px', height: '38px', marginTop: '20px', fontSize: '12px' }}
                      onClick={() => alert('Webhook settings updated!')}
                    >
                      Update webhook settings
                    </button>
                  </div>
                </div>

                {/* Right Column: Webhook Log & cURL Box */}
                <div className="dev-col">
                  {/* Recent Deliveries */}
                  <div className="dev-card">
                    <div className="dev-card-head">
                      <h2 className="dev-card-title">Recent deliveries</h2>
                      <button
                        type="button"
                        className="button button-outline"
                        style={{ width: 'auto', height: '30px', padding: '0 10px', fontSize: '10px' }}
                        onClick={() => {
                          setTestSent(true);
                          setTimeout(() => setTestSent(false), 2500);
                        }}
                      >
                        {testSent ? '✓ Sent!' : 'Send test event'}
                      </button>
                    </div>
                    <p className="dev-card-desc">Last 3 webhook delivery attempts to your server.</p>

                    <div className="dev-log-list">
                      <div className="dev-log-item">
                        <div>
                          <span className="status-pill delivered" style={{ marginRight: '8px' }}>200 OK</span>
                          <strong>parcel.delivered</strong>
                          <span style={{ color: 'var(--muted)', marginLeft: '6px' }}>SD-20455</span>
                        </div>
                        <span style={{ color: 'var(--muted)' }}>2 mins ago</span>
                      </div>

                      <div className="dev-log-item">
                        <div>
                          <span className="status-pill delivered" style={{ marginRight: '8px' }}>200 OK</span>
                          <strong>parcel.in_transit</strong>
                          <span style={{ color: 'var(--muted)', marginLeft: '6px' }}>SD-20476</span>
                        </div>
                        <span style={{ color: 'var(--muted)' }}>18 mins ago</span>
                      </div>

                      <div className="dev-log-item">
                        <div>
                          <span className="status-pill failed" style={{ marginRight: '8px' }}>500 ERR</span>
                          <strong>parcel.created</strong>
                          <span style={{ color: 'var(--muted)', marginLeft: '6px' }}>SD-20479</span>
                        </div>
                        <button
                          type="button"
                          style={{ border: 'none', background: 'none', color: 'var(--orange-dark)', fontWeight: 700, fontSize: '10px', cursor: 'pointer' }}
                          onClick={() => alert('Retried webhook for SD-20479')}
                        >
                          Retry
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* cURL Code Snippet Box */}
                  <div className="dev-card">
                    <div className="dev-card-head">
                      <h2 className="dev-card-title">Create an order by API</h2>
                      <button
                        type="button"
                        className="button button-outline"
                        style={{ width: 'auto', height: '30px', padding: '0 10px', fontSize: '10px' }}
                        onClick={() => copyToClipboard(curlSnippet, 'curl')}
                      >
                        {copiedCurl ? '✓ Copied' : 'Copy cURL'}
                      </button>
                    </div>
                    <p className="dev-card-desc">Use this endpoint to book a parcel from your backend.</p>

                    <pre className="dev-code-box">
                      <code>{curlSnippet}</code>
                    </pre>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Create Order Modal Dialog */}
      {showCreateModal && (
        <div className="modal-backdrop" onClick={() => setShowCreateModal(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <h2 style={{ fontSize: '18px', fontWeight: 800, margin: 0 }}>Create a new parcel delivery</h2>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer', color: 'var(--muted)' }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateOrder}>
              <div className="reg-grid-2">
                <div className="reg-form-group">
                  <label htmlFor="modalRecipientName">Recipient Name *</label>
                  <input
                    id="modalRecipientName"
                    required
                    type="text"
                    value={newOrder.recipientName}
                    onChange={(e) => setNewOrder({ ...newOrder, recipientName: e.target.value })}
                    placeholder="e.g. Sunil Fernando"
                  />
                </div>
                <div className="reg-form-group">
                  <label htmlFor="modalRecipientPhone">Recipient Phone *</label>
                  <input
                    id="modalRecipientPhone"
                    required
                    type="tel"
                    value={newOrder.recipientPhone}
                    onChange={(e) => setNewOrder({ ...newOrder, recipientPhone: e.target.value })}
                    placeholder="+94 77 123 4567"
                  />
                </div>
              </div>

              <div className="reg-form-group">
                <label htmlFor="modalDropoffAddress">Dropoff Address *</label>
                <input
                  id="modalDropoffAddress"
                  required
                  type="text"
                  value={newOrder.dropoffAddress}
                  onChange={(e) => setNewOrder({ ...newOrder, dropoffAddress: e.target.value })}
                  placeholder="e.g. 12 Temple Road, Galle"
                />
              </div>

              <div className="reg-grid-2">
                <div className="reg-form-group">
                  <label htmlFor="modalDropoffZone">Dropoff Zone *</label>
                  <select
                    id="modalDropoffZone"
                    value={newOrder.dropoffZone}
                    onChange={(e) => setNewOrder({ ...newOrder, dropoffZone: e.target.value })}
                  >
                    <option value="COLOMBO_SOUTH">Colombo South</option>
                    <option value="COLOMBO_CENTRAL">Colombo Central</option>
                    <option value="COLOMBO_NORTH">Colombo North</option>
                    <option value="INTER_CITY">Inter-city (Galle, Kandy)</option>
                  </select>
                </div>
                <div className="reg-form-group">
                  <label htmlFor="modalWeight">Weight (kg) *</label>
                  <input
                    id="modalWeight"
                    type="number"
                    step="0.1"
                    min="0.1"
                    value={newOrder.weightKg}
                    onChange={(e) => setNewOrder({ ...newOrder, weightKg: parseFloat(e.target.value) || 1 })}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '20px', justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  className="button button-outline"
                  style={{ width: 'auto', padding: '0 18px' }}
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingOrder}
                  className="button button-primary"
                  style={{ width: 'auto', padding: '0 24px' }}
                >
                  {creatingOrder ? 'Booking…' : 'Book Delivery'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
