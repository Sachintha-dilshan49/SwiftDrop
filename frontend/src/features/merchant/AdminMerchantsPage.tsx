import React, { useCallback, useEffect, useState } from 'react';
import { merchantApi, MerchantResponse } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  adminToken?: string;
}

/** Up to this many merchants are listed per section. */
const PAGE_SIZE = 100;

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0])
    .join('')
    .toUpperCase();
}

function formatDate(iso?: string): string {
  if (!iso) return '–';
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(iso));
}

/** The admin's merchant screen: applications to approve or reject, and the approved merchants. */
export const AdminMerchantsPage: React.FC<Props> = ({ adminToken }) => {
  const [pending, setPending] = useState<MerchantResponse[]>([]);
  const [approved, setApproved] = useState<MerchantResponse[]>([]);
  const [loading, setLoading] = useState(Boolean(adminToken));
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [busyId, setBusyId] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<{ id: string; reason: string } | null>(null);

  const load = useCallback(async () => {
    if (!adminToken) return;
    setLoading(true);
    setError(null);
    try {
      const [pendingPage, approvedPage] = await Promise.all([
        merchantApi.listPendingMerchants(adminToken, 'PENDING', 0, PAGE_SIZE),
        merchantApi.listPendingMerchants(adminToken, 'APPROVED', 0, PAGE_SIZE),
      ]);
      setPending(pendingPage?.content ?? []);
      setApproved(approvedPage?.content ?? []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Could not load the merchants. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [adminToken]);

  useEffect(() => {
    void load();
  }, [load]);

  const approve = async (merchant: MerchantResponse) => {
    if (!adminToken) return;
    setBusyId(merchant.id);
    setError(null);
    try {
      await merchantApi.approveMerchant(adminToken, merchant.id);
      setNotice(`${merchant.businessName} is approved and can now sign in to the merchant portal.`);
      await load();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Approval failed. Please try again.');
    } finally {
      setBusyId(null);
    }
  };

  const reject = async (merchant: MerchantResponse, reason: string) => {
    if (!adminToken) return;
    setBusyId(merchant.id);
    setError(null);
    try {
      await merchantApi.rejectMerchant(adminToken, merchant.id, reason.trim());
      setRejecting(null);
      setNotice(`${merchant.businessName}'s application was rejected.`);
      await load();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Rejection failed. Please try again.');
    } finally {
      setBusyId(null);
    }
  };

  const query = searchQuery.trim().toLowerCase();
  const visibleApproved = approved.filter(
    (m) =>
      !query ||
      m.businessName.toLowerCase().includes(query) ||
      m.businessRegNo.toLowerCase().includes(query) ||
      m.contactName.toLowerCase().includes(query)
  );

  return (
    <div className="merchant-body">
      <div className="merchant-page-head">
        <div>
          <h1>Merchants</h1>
          <p>Review business applications and see the approved merchant accounts.</p>
        </div>
      </div>

      {notice && (
        <div className="alert merchant-notice" role="status">
          ✓ {notice}
        </div>
      )}
      {error && (
        <div className="alert" role="alert">
          {error}
        </div>
      )}

      <section className="merchant-admin-section" aria-labelledby="approval-queue-title">
        <div className="merchant-admin-heading">
          <h2 id="approval-queue-title">Approval queue</h2>
          <span className="merchant-role-tag merchant-count-tag">{pending.length} waiting</span>
        </div>

        {loading ? (
          <p className="merchant-muted">Loading merchants…</p>
        ) : pending.length === 0 ? (
          <div className="dev-card merchant-empty">✓ No applications are waiting for review.</div>
        ) : (
          <div className="merchant-queue-grid">
            {pending.map((m) => {
              const isRejecting = rejecting?.id === m.id;
              return (
                <article key={m.id} className="dev-card merchant-queue-card" aria-label={m.businessName}>
                  <div>
                    <div className="merchant-queue-title">
                      <div className="merchant-avatar">{initials(m.businessName)}</div>
                      <div>
                        <strong>{m.businessName}</strong>
                        <span>Applied {formatDate(m.createdAt)}</span>
                      </div>
                    </div>
                    <dl className="merchant-queue-details">
                      <div><dt>BRN</dt><dd>{m.businessRegNo}</dd></div>
                      <div><dt>Contact</dt><dd>{m.contactName}, {m.phone}</dd></div>
                      <div><dt>Email</dt><dd>{m.email}</dd></div>
                      <div><dt>Address</dt><dd>{m.address}</dd></div>
                    </dl>
                  </div>

                  {isRejecting ? (
                    <form
                      className="merchant-reject-form"
                      onSubmit={(event) => {
                        event.preventDefault();
                        if (rejecting.reason.trim()) void reject(m, rejecting.reason);
                      }}
                    >
                      <label htmlFor={`reject-${m.id}`}>Reason for rejecting (shown to the merchant)</label>
                      <textarea
                        id={`reject-${m.id}`}
                        value={rejecting.reason}
                        maxLength={500}
                        rows={2}
                        onChange={(event) => setRejecting({ id: m.id, reason: event.target.value })}
                      />
                      <div className="merchant-queue-actions">
                        <button type="button" className="button button-outline" onClick={() => setRejecting(null)}>
                          Cancel
                        </button>
                        <button
                          type="submit"
                          className="button button-primary merchant-danger"
                          disabled={!rejecting.reason.trim() || busyId === m.id}
                        >
                          Reject application
                        </button>
                      </div>
                    </form>
                  ) : (
                    <div className="merchant-queue-actions">
                      <button
                        type="button"
                        className="button button-outline"
                        disabled={busyId === m.id}
                        onClick={() => setRejecting({ id: m.id, reason: '' })}
                      >
                        Reject
                      </button>
                      <button
                        type="button"
                        className="button button-primary"
                        disabled={busyId === m.id}
                        onClick={() => void approve(m)}
                      >
                        {busyId === m.id ? 'Approving…' : 'Approve'}
                      </button>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section aria-labelledby="approved-title">
        <div className="merchant-admin-heading merchant-admin-heading-split">
          <h2 id="approved-title">Approved merchants</h2>
          <div className="merchant-search-wrapper">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="search"
              aria-label="Search approved merchants"
              placeholder="Search approved merchants…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        <div className="merchant-card-box">
          <table className="merchant-table">
            <thead>
              <tr>
                <th>Merchant</th>
                <th>BRN</th>
                <th>Contact</th>
                <th>Email</th>
                <th>Approved</th>
              </tr>
            </thead>
            <tbody>
              {!loading && visibleApproved.length === 0 && (
                <tr>
                  <td colSpan={5} className="merchant-muted">
                    {approved.length === 0 ? 'No approved merchants yet.' : 'No merchants match your search.'}
                  </td>
                </tr>
              )}
              {visibleApproved.map((m) => (
                <tr key={m.id}>
                  <td><strong>{m.businessName}</strong></td>
                  <td><code>{m.businessRegNo}</code></td>
                  <td>{m.contactName}<br /><span className="merchant-muted">{m.phone}</span></td>
                  <td>{m.email}</td>
                  <td>{formatDate(m.updatedAt ?? m.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
