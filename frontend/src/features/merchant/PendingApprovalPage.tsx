import React, { useCallback, useEffect, useState } from 'react';
import { merchantApi, MerchantResponse } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  token?: string;
  /** Approved: open the merchant portal (the caller refreshes the session so it carries the MERCHANT role). */
  onOpenPortal?: () => void | Promise<void>;
  /** No application yet: go to the registration form. */
  onRegister?: () => void;
}

const DATE_TIME = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Colombo', day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit',
  hour12: true,
});

/** The merchant's application: waiting for review, approved or rejected, with what they submitted. */
export const PendingApprovalPage: React.FC<Props> = ({ token, onOpenPortal, onRegister }) => {
  const [profile, setProfile] = useState<MerchantResponse | null>(null);
  const [notApplied, setNotApplied] = useState(false);
  const [loading, setLoading] = useState(Boolean(token));
  const [opening, setOpening] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      setProfile(await merchantApi.getMyProfile(token));
      setNotApplied(false);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : '';
      // merchant-service answers 404 "Merchant profile not found" when this account has not applied
      if (/not found/i.test(message)) {
        setNotApplied(true);
      } else {
        setError(message || 'Could not load your application. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void fetchStatus();
  }, [fetchStatus]);

  const openPortal = async () => {
    if (!onOpenPortal) return;
    setOpening(true);
    setError(null);
    try {
      await onOpenPortal();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Could not open the merchant portal. Please sign in again.');
      setOpening(false);
    }
  };

  if (loading) {
    return (
      <div className="pending-shell" style={{ display: 'grid', placeItems: 'center' }}>
        <div className="loading-indicator" aria-label="Loading application status" />
      </div>
    );
  }

  if (notApplied || (!profile && !error)) {
    return (
      <div className="pending-shell">
        <div className="pending-card">
          <h1 className="pending-title">You have not applied yet</h1>
          <p className="pending-lead">Apply for a merchant account to send your store's orders with SwiftDrop.</p>
          {onRegister && (
            <button type="button" className="button button-primary pending-action" onClick={onRegister}>
              Apply as a merchant
            </button>
          )}
        </div>
      </div>
    );
  }

  const isApproved = profile?.status === 'APPROVED';
  const isRejected = profile?.status === 'REJECTED';

  return (
    <div className="pending-shell">
      <div className="pending-card">
        <div className={`pending-icon-circle ${isApproved ? 'is-approved' : isRejected ? 'is-rejected' : 'is-pending'}`} aria-hidden="true">
          {isApproved ? (
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          ) : isRejected ? (
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          ) : (
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          )}
        </div>

        <h1 className="pending-title">
          {isApproved ? 'Your account is approved' : isRejected ? 'Your application was not approved' : 'Your application is being reviewed'}
        </h1>
        <p className="pending-lead">
          {isApproved
            ? `${profile?.businessName} can now send orders with SwiftDrop.`
            : isRejected
            ? `Reason: ${profile?.rejectionReason || 'not given'}. Contact SwiftDrop support if you have questions.`
            : 'A SwiftDrop admin checks every application. This page shows the result as soon as it is decided.'}
        </p>

        {error && <div className="alert" role="alert">{error}</div>}

        {profile && (
          <>
            <ol className="pending-timeline">
              <li className="pending-step-item">
                <div className="pending-step-bullet done">✓</div>
                <div>
                  <strong>Application submitted</strong>
                  <span>{DATE_TIME.format(new Date(profile.createdAt))}</span>
                </div>
              </li>
              <li className="pending-step-item">
                <div className={`pending-step-bullet ${isApproved ? 'done' : isRejected ? 'idle' : 'active'}`}>
                  {isApproved ? '✓' : isRejected ? '✕' : '2'}
                </div>
                <div>
                  <strong>Review by the SwiftDrop team</strong>
                  <span>
                    {isApproved || isRejected
                      ? `Decided${profile.updatedAt ? ` ${DATE_TIME.format(new Date(profile.updatedAt))}` : ''}`
                      : 'Waiting for an admin'}
                  </span>
                </div>
              </li>
              <li className="pending-step-item">
                <div className={`pending-step-bullet ${isApproved ? 'done' : 'idle'}`}>{isApproved ? '✓' : '3'}</div>
                <div>
                  <strong>Start shipping</strong>
                  <span>Create an API key and send orders from your store</span>
                </div>
              </li>
            </ol>

            <dl className="pending-details">
              <div><dt>Business name</dt><dd>{profile.businessName}</dd></div>
              <div><dt>BR number</dt><dd>{profile.businessRegNo}</dd></div>
              <div><dt>Contact</dt><dd>{profile.contactName}, {profile.phone}</dd></div>
              <div><dt>Pickup address</dt><dd>{profile.address}</dd></div>
            </dl>
          </>
        )}

        {isApproved ? (
          <button type="button" className="button button-primary pending-action" disabled={opening} onClick={() => void openPortal()}>
            {opening ? 'Opening…' : 'Open the merchant portal'}
          </button>
        ) : !isRejected ? (
          <button type="button" className="button button-outline pending-action" onClick={() => void fetchStatus()}>
            Check again
          </button>
        ) : null}
      </div>
    </div>
  );
};
