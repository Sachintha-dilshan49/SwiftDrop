import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantResponse } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  token?: string;
  onNavigateToDashboard?: () => void;
  onNavigateToAdmin?: () => void;
}

export const PendingApprovalPage: React.FC<Props> = ({
  token,
  onNavigateToDashboard,
  onNavigateToAdmin,
}) => {
  const [profile, setProfile] = useState<MerchantResponse | null>(null);
  const [loading, setLoading] = useState(Boolean(token));
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = async () => {
    if (!token) return;
    try {
      setLoading(true);
      setError(null);
      const res = await merchantApi.getMyProfile(token);
      setProfile(res);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unable to fetch status';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      fetchStatus();
    }
  }, [token]);

  // Fallback demo data matching Figma frame 1:1652
  const merchantData: MerchantResponse = profile || {
    id: 'demo-merchant-id',
    businessName: 'Lotus Leaf Bakery (Pvt) Ltd',
    businessRegNo: 'PV-104928',
    contactName: 'Ayesha Rodrigo',
    email: 'ayesha@lotusleaf.lk',
    phone: '+94 77 123 4567',
    address: 'No. 42, Ward Place, Colombo 07',
    status: 'PENDING',
    createdAt: '2026-10-29T10:14:00Z',
  };

  const isApproved = merchantData.status === 'APPROVED';
  const isRejected = merchantData.status === 'REJECTED';

  if (loading) {
    return (
      <div className="pending-shell" style={{ display: 'grid', placeItems: 'center' }}>
        <div className="loading-indicator" aria-label="Loading application status" />
      </div>
    );
  }

  return (
    <div className="pending-shell">
      {/* Top Header */}
      <div style={{ maxWidth: '660px', margin: '0 auto 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div className="brand-mark" style={{ width: '28px', height: '28px', background: 'var(--ink)', color: 'var(--orange)', borderRadius: '7px 7px 7px 2px', display: 'grid', placeItems: 'center' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          </div>
          <span style={{ fontSize: '18px', fontWeight: 800, letterSpacing: '-0.03em', color: 'var(--ink)' }}>SwiftDrop</span>
          <span className="merchant-role-tag">Merchant</span>
        </div>
        <span style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600 }}>
          {merchantData.businessName}
        </span>
      </div>

      <div className="pending-card">
        {/* Status Icon */}
        <div
          className="pending-icon-circle"
          style={{
            background: isApproved ? '#e6f6ee' : isRejected ? '#fdeeee' : '#fff4ec',
            color: isApproved ? '#167a49' : isRejected ? '#b32a2a' : 'var(--orange)',
          }}
        >
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

        <h1 style={{ fontSize: '24px', fontWeight: 800, margin: '0 0 8px', letterSpacing: '-0.03em' }}>
          {isApproved
            ? 'Your account is approved!'
            : isRejected
            ? 'Application requires attention'
            : 'Your application is being reviewed'}
        </h1>

        <p style={{ color: 'var(--muted)', fontSize: '13px', margin: '0 0 24px', lineHeight: 1.5 }}>
          {isApproved
            ? `Welcome to SwiftDrop! Your account for ${merchantData.businessName} has been verified.`
            : isRejected
            ? `Reason: ${merchantData.rejectionReason || 'Please verify documents with SwiftDrop operations.'}`
            : 'Submitted on Oct 29, 2026 · We typically review and approve merchant accounts within 24 business hours.'}
        </p>

        {error && (
          <div className="alert" role="alert" style={{ marginBottom: '16px' }}>
            {error}
          </div>
        )}

        {/* 3-Step Verification Timeline matching Figma 1:1652 */}
        <div className="pending-timeline">
          <div className="pending-step-item">
            <div className="pending-step-bullet done">✓</div>
            <div>
              <strong style={{ fontSize: '13px', display: 'block', color: 'var(--ink)' }}>1. Application submitted</strong>
              <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Oct 29, 10:14 AM · Received</span>
            </div>
          </div>

          <div className="pending-step-item">
            <div className={`pending-step-bullet ${isApproved ? 'done' : isRejected ? 'idle' : 'active'}`}>
              {isApproved ? '✓' : '2'}
            </div>
            <div>
              <strong style={{ fontSize: '13px', display: 'block', color: 'var(--ink)' }}>2. Documents check by SwiftDrop team</strong>
              <span style={{ fontSize: '11px', color: 'var(--muted)' }}>
                {isApproved
                  ? 'BR certificate & NIC verified'
                  : 'BR certificate & NIC being verified by operations staff'}
              </span>
            </div>
          </div>

          <div className="pending-step-item">
            <div className={`pending-step-bullet ${isApproved ? 'done' : 'idle'}`}>
              {isApproved ? '✓' : '3'}
            </div>
            <div>
              <strong style={{ fontSize: '13px', display: 'block', color: 'var(--ink)' }}>3. Account activated · start shipping</strong>
              <span style={{ fontSize: '11px', color: 'var(--muted)' }}>
                {isApproved ? 'API keys active and warehouse courier assigned' : 'API keys issued and courier assigned to your warehouse'}
              </span>
            </div>
          </div>
        </div>

        {/* Summary Card */}
        <div style={{ textAlign: 'left', background: '#fafbfc', border: '1px solid var(--line)', borderRadius: '10px', padding: '16px 20px', marginBottom: '20px' }}>
          <h2 style={{ fontSize: '12px', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--muted)', margin: '0 0 12px' }}>
            Application details
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '12px' }}>
            <div>
              <span style={{ color: 'var(--muted)', display: 'block', fontSize: '10px' }}>Business Name</span>
              <strong style={{ color: 'var(--ink)' }}>{merchantData.businessName}</strong>
            </div>
            <div>
              <span style={{ color: 'var(--muted)', display: 'block', fontSize: '10px' }}>BR Number</span>
              <strong style={{ color: 'var(--ink)' }}>{merchantData.businessRegNo}</strong>
            </div>
            <div>
              <span style={{ color: 'var(--muted)', display: 'block', fontSize: '10px' }}>Category</span>
              <strong style={{ color: 'var(--ink)' }}>Bakery & Food</strong>
            </div>
            <div>
              <span style={{ color: 'var(--muted)', display: 'block', fontSize: '10px' }}>Pickup Warehouse</span>
              <strong style={{ color: 'var(--ink)' }}>{merchantData.address}</strong>
            </div>
          </div>
        </div>

        {/* Demo banner matching Figma callout */}
        <div className="demo-callout-banner">
          <strong style={{ fontSize: '12px', color: 'var(--orange-dark)', display: 'block', marginBottom: '4px' }}>
            Demo & Evaluation Note
          </strong>
          <p style={{ fontSize: '11px', color: '#684524', margin: '0 0 12px', lineHeight: 1.45 }}>
            To approve this merchant application in this demo environment, switch to the <strong>Admin portal → Merchants</strong>, or preview the merchant dashboard directly.
          </p>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {onNavigateToAdmin && (
              <button
                type="button"
                className="button button-primary"
                style={{ width: 'auto', minHeight: '36px', fontSize: '11px', padding: '0 16px' }}
                onClick={onNavigateToAdmin}
              >
                Open Admin Portal →
              </button>
            )}
            {onNavigateToDashboard && (
              <button
                type="button"
                className="button button-outline"
                style={{ width: 'auto', minHeight: '36px', fontSize: '11px', padding: '0 16px' }}
                onClick={onNavigateToDashboard}
              >
                View Dashboard Preview
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
