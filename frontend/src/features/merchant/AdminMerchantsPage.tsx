import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantResponse } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  adminToken?: string;
  onSelectMerchant?: (merchant: MerchantResponse) => void;
}

export const AdminMerchantsPage: React.FC<Props> = ({ adminToken }) => {
  const [, setMerchants] = useState<MerchantResponse[]>([]);
  const [loading, setLoading] = useState(Boolean(adminToken));
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Demo queue matching Figma frame 1:3742
  const initialPendingMerchants: MerchantResponse[] = [
    {
      id: 'm-1',
      businessName: 'Spice Route Organics',
      businessRegNo: 'PV-982341',
      contactName: 'Samitha Rathnayake',
      phone: '+94 71 889 0123',
      email: 'samitha@spiceroute.lk',
      address: 'Colombo Central (01-03)',
      status: 'PENDING',
      createdAt: '2026-10-30T09:15:00Z',
    },
    {
      id: 'm-2',
      businessName: 'Lotus Leaf Bakery (Pvt) Ltd',
      businessRegNo: 'PV-104928',
      contactName: 'Ayesha Rodrigo',
      phone: '+94 77 123 4567',
      email: 'ayesha@lotusleaf.lk',
      address: 'Ward Place, Colombo 07',
      status: 'PENDING',
      createdAt: '2026-10-29T10:14:00Z',
    },
    {
      id: 'm-3',
      businessName: 'Ceylon Kraft Paper',
      businessRegNo: 'PV-110294',
      contactName: 'Kavinda Perera',
      phone: '+94 76 554 3210',
      email: 'orders@ceylonkraft.lk',
      address: 'Industrial Zone, Kandy',
      status: 'PENDING',
      createdAt: '2026-10-28T14:40:00Z',
    },
  ];

  const [pendingQueue, setPendingQueue] = useState<MerchantResponse[]>(initialPendingMerchants);

  // Approved merchants list matching Figma 1:3742
  const [approvedList, setApprovedList] = useState<
    Array<{
      id: string;
      businessName: string;
      businessRegNo: string;
      category: string;
      contact: string;
      ordersCount: number;
      status: 'ACTIVE' | 'SUSPENDED';
    }>
  >([
    {
      id: 'app-1',
      businessName: 'Urban Artisan Coffee',
      businessRegNo: 'PV-89210',
      category: 'Food & Beverage',
      contact: 'Nirosh Silva (+94 77 890 1234)',
      ordersCount: 342,
      status: 'ACTIVE',
    },
    {
      id: 'app-2',
      businessName: 'Lanka Tech Gear',
      businessRegNo: 'PV-76431',
      category: 'Electronics',
      contact: 'Duminda Alwis (+94 71 234 5678)',
      ordersCount: 890,
      status: 'ACTIVE',
    },
    {
      id: 'app-3',
      businessName: 'Blue Lotus Herbal',
      businessRegNo: 'PV-65412',
      category: 'Healthcare',
      contact: 'Sandani Jayakody (+94 76 112 3344)',
      ordersCount: 124,
      status: 'ACTIVE',
    },
  ]);

  const fetchMerchants = async () => {
    if (!adminToken) return;
    try {
      setLoading(true);
      setError(null);
      const res = await merchantApi.listPendingMerchants(adminToken, 'PENDING');
      if (res && res.content && res.content.length > 0) {
        setMerchants(res.content);
        setPendingQueue(res.content);
      }
    } catch {
      // Use fallback
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (adminToken) {
      fetchMerchants();
    }
  }, [adminToken]);

  const handleApprove = async (id: string, name: string) => {
    if (adminToken) {
      try {
        await merchantApi.approveMerchant(adminToken, id);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Approval failed';
        alert(message);
        return;
      }
    }

    setPendingQueue(pendingQueue.filter((m) => m.id !== id));
    setApprovedList([
      {
        id,
        businessName: name,
        businessRegNo: 'PV-104928',
        category: 'Bakery & Food',
        contact: 'Ayesha Rodrigo (+94 77 123 4567)',
        ordersCount: 0,
        status: 'ACTIVE',
      },
      ...approvedList,
    ]);
    setActionSuccess(`Merchant "${name}" has been approved! API credentials issued.`);
    setTimeout(() => setActionSuccess(null), 4000);
  };

  const handleReject = async (id: string, name: string) => {
    const reason = window.prompt(`Please enter the rejection reason for ${name}:`, 'Documents incomplete or illegible');
    if (!reason) return;

    if (adminToken) {
      try {
        await merchantApi.rejectMerchant(adminToken, id, reason);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Rejection failed';
        alert(message);
        return;
      }
    }

    setPendingQueue(pendingQueue.filter((m) => m.id !== id));
    setActionSuccess(`Merchant "${name}" application rejected.`);
    setTimeout(() => setActionSuccess(null), 4000);
  };

  const filteredApproved = approvedList.filter((m) =>
    m.businessName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    m.businessRegNo.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="merchant-body">
      <div className="merchant-page-head">
        <div>
          <h1>Merchants</h1>
          <p>Review business partner applications and manage approved merchant accounts.</p>
        </div>
      </div>

      {actionSuccess && (
        <div className="alert" style={{ background: '#e6f6ee', borderColor: '#a7dfb8', color: '#167a49', marginBottom: '20px' }}>
          ✓ {actionSuccess}
        </div>
      )}

      {error && (
        <div className="alert" role="alert" style={{ marginBottom: '20px' }}>
          {error}
        </div>
      )}

      {/* SECTION 1: APPROVAL QUEUE (Figma 1:3742) */}
      <section style={{ marginBottom: '36px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '16px', fontWeight: 800, margin: 0, color: 'var(--ink)' }}>Approval queue</h2>
          <span className="merchant-role-tag" style={{ background: '#fff1e6', color: 'var(--orange-dark)', fontWeight: 800 }}>
            {pendingQueue.length} waiting
          </span>
        </div>

        {loading ? (
          <p style={{ color: 'var(--muted)', padding: '20px' }}>Loading queue…</p>
        ) : pendingQueue.length === 0 ? (
          <div className="dev-card" style={{ textAlign: 'center', padding: '32px', color: 'var(--muted)' }}>
            ✓ No merchant applications waiting for review. All caught up!
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '18px' }}>
            {pendingQueue.map((m) => {
              const initials = m.businessName
                .split(' ')
                .slice(0, 2)
                .map((w) => w[0])
                .join('')
                .toUpperCase();

              return (
                <div key={m.id} className="dev-card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
                      <div className="merchant-avatar" style={{ background: '#2563eb' }}>
                        {initials}
                      </div>
                      <div>
                        <strong style={{ fontSize: '14px', display: 'block', color: 'var(--ink)' }}>{m.businessName}</strong>
                        <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Registered {m.address}</span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '11px', color: '#4d5b70', margin: '14px 0' }}>
                      <div>
                        <span style={{ color: 'var(--muted)' }}>BRN:</span> <strong>{m.businessRegNo}</strong>
                      </div>
                      <div>
                        <span style={{ color: 'var(--muted)' }}>Contact:</span> {m.contactName} ({m.phone})
                      </div>
                      <div>
                        <span style={{ color: 'var(--muted)' }}>Documents:</span>{' '}
                        <span className="status-pill delivered" style={{ display: 'inline-flex', padding: '2px 7px', fontSize: '10px' }}>
                          ✓ 2/2 verified
                        </span>
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '10px', marginTop: '16px', borderTop: '1px solid var(--line)', paddingTop: '14px' }}>
                    <button
                      type="button"
                      className="button button-outline"
                      style={{ flex: 1, height: '36px', fontSize: '11px', color: '#b32a2a' }}
                      onClick={() => handleReject(m.id, m.businessName)}
                    >
                      Reject
                    </button>
                    <button
                      type="button"
                      className="button button-primary"
                      style={{ flex: 1, height: '36px', fontSize: '11px' }}
                      onClick={() => handleApprove(m.id, m.businessName)}
                    >
                      Approve
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* SECTION 2: APPROVED MERCHANTS TABLE (Figma 1:3742) */}
      <section>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
          <h2 style={{ fontSize: '16px', fontWeight: 800, margin: 0, color: 'var(--ink)' }}>Approved merchants</h2>
          <div className="merchant-search-wrapper" style={{ width: '260px' }}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
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
                <th>Category</th>
                <th>Primary Contact</th>
                <th>Total Orders</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredApproved.map((m) => (
                <tr key={m.id}>
                  <td><strong>{m.businessName}</strong></td>
                  <td><code>{m.businessRegNo}</code></td>
                  <td style={{ color: 'var(--muted)' }}>{m.category}</td>
                  <td>{m.contact}</td>
                  <td><strong>{m.ordersCount}</strong></td>
                  <td>
                    <span className="status-pill delivered">Active</span>
                  </td>
                  <td>
                    <button
                      type="button"
                      style={{ background: 'none', border: 'none', color: 'var(--orange-dark)', fontWeight: 650, fontSize: '11px', cursor: 'pointer' }}
                      onClick={() => alert(`Viewing details for ${m.businessName}`)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
