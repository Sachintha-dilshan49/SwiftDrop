import React, { useState } from 'react';
import { merchantApi, RegisterMerchantRequest } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  token?: string;
  onBackToSignIn?: () => void;
  onRegistered?: (businessName: string, regNo: string) => void;
}

export const MerchantRegisterPage: React.FC<Props> = ({ token, onBackToSignIn, onRegistered }) => {
  const [formData, setFormData] = useState<RegisterMerchantRequest & { category?: string; zone?: string }>({
    businessName: '',
    businessRegNo: '',
    contactName: '',
    email: '',
    phone: '',
    address: '',
    category: 'Bakery & Food',
    zone: 'COLOMBO_SOUTH',
  });
  const [hasBrDoc, setHasBrDoc] = useState(false);
  const [hasNicDoc, setHasNicDoc] = useState(false);
  const [agreedTerms, setAgreedTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const fillSampleData = () => {
    setFormData({
      businessName: 'Lotus Leaf Bakery (Pvt) Ltd',
      businessRegNo: 'PV-104928',
      contactName: 'Ayesha Rodrigo',
      email: 'ayesha@lotusleaf.lk',
      phone: '+94 77 123 4567',
      address: 'No. 42, Ward Place, Colombo 07',
      category: 'Bakery & Food',
      zone: 'COLOMBO_SOUTH',
    });
    setHasBrDoc(true);
    setHasNicDoc(true);
    setAgreedTerms(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreedTerms) {
      setError('Please agree to the Merchant Terms & Conditions.');
      return;
    }
    setLoading(true);
    setError(null);

    try {
      await merchantApi.register({
        businessName: formData.businessName,
        businessRegNo: formData.businessRegNo,
        contactName: formData.contactName,
        email: formData.email,
        phone: formData.phone,
        address: formData.address,
      }, token);
      setSubmitted(true);
      if (onRegistered) {
        onRegistered(formData.businessName, formData.businessRegNo);
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to submit registration';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="pending-shell">
        <div className="pending-card">
          <div className="pending-icon-circle">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </div>
          <h1 style={{ fontSize: '26px', fontWeight: 800, margin: '0 0 8px', letterSpacing: '-0.03em' }}>
            Application submitted!
          </h1>
          <p style={{ color: 'var(--muted)', fontSize: '13px', margin: '0 0 24px', lineHeight: 1.5 }}>
            Thank you for registering <strong>{formData.businessName}</strong>. Our operations team is reviewing your business registration and identity documents.
          </p>

          <div className="pending-timeline">
            <div className="pending-step-item">
              <div className="pending-step-bullet done">✓</div>
              <div>
                <strong style={{ fontSize: '13px', display: 'block' }}>Application submitted</strong>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Just now · Verification queued</span>
              </div>
            </div>
            <div className="pending-step-item">
              <div className="pending-step-bullet active">2</div>
              <div>
                <strong style={{ fontSize: '13px', display: 'block' }}>Documents check by SwiftDrop team</strong>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>BR certificate & NIC being verified</span>
              </div>
            </div>
            <div className="pending-step-item">
              <div className="pending-step-bullet idle">3</div>
              <div>
                <strong style={{ fontSize: '13px', display: 'block' }}>Account activated · start shipping</strong>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>API keys issued and courier assigned</span>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
            <button
              type="button"
              className="button button-primary"
              style={{ width: 'auto', padding: '0 24px' }}
              onClick={() => {
                if (onBackToSignIn) onBackToSignIn();
                else window.location.href = '/merchant';
              }}
            >
              Go to Merchant Portal
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="reg-shell">
      <div className="reg-container">
        {/* Top Header */}
        <div className="reg-top-bar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div className="brand-mark" style={{ width: '28px', height: '28px', background: 'var(--ink)', color: 'var(--orange)', borderRadius: '7px 7px 7px 2px', display: 'grid', placeItems: 'center' }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
              </svg>
            </div>
            <span style={{ fontSize: '18px', fontWeight: 800, letterSpacing: '-0.03em', color: 'var(--ink)' }}>SwiftDrop</span>
            <span className="merchant-role-tag">Merchant</span>
          </div>

          {onBackToSignIn && (
            <button type="button" className="reg-back-link" onClick={onBackToSignIn}>
              ← Back to sign in
            </button>
          )}
        </div>

        {/* Title Section */}
        <div className="reg-header">
          <p className="eyebrow" style={{ color: 'var(--orange-dark)', fontWeight: 800, fontSize: '10px', letterSpacing: '0.12em', margin: 0 }}>
            MERCHANT REGISTRATION
          </p>
          <h1>Register your business</h1>
          <p>Deliver across Colombo with dedicated couriers, COD collection and transparent per-km pricing. Verification takes under 24 hours.</p>
        </div>

        {error && (
          <div className="alert" role="alert" style={{ marginBottom: '20px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Card 1: Business Details */}
          <div className="reg-card">
            <h2 className="reg-card-title">1. Business details</h2>

            <div className="reg-form-group">
              <label htmlFor="businessName">Business Name *</label>
              <input
                id="businessName"
                name="businessName"
                type="text"
                required
                value={formData.businessName}
                onChange={handleChange}
                placeholder="e.g. Lotus Leaf Bakery (Pvt) Ltd"
              />
            </div>

            <div className="reg-form-group">
              <label htmlFor="businessRegNo">Business Registration Number (BRN) *</label>
              <input
                id="businessRegNo"
                name="businessRegNo"
                type="text"
                required
                value={formData.businessRegNo}
                onChange={handleChange}
                placeholder="e.g. PV-104928"
              />
              <p className="reg-hint">Your registered PV or sole-proprietor business registration number.</p>
            </div>

            <div className="reg-grid-2">
              <div className="reg-form-group">
                <label htmlFor="category">Business Category *</label>
                <select id="category" name="category" value={formData.category} onChange={handleChange}>
                  <option value="Bakery & Food">Bakery & Food</option>
                  <option value="E-commerce & Retail">E-commerce & Retail</option>
                  <option value="Pharmacy & Healthcare">Pharmacy & Healthcare</option>
                  <option value="Fashion & Apparel">Fashion & Apparel</option>
                  <option value="Electronics">Electronics</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div className="reg-form-group">
                <label htmlFor="zone">Primary Pickup Zone *</label>
                <select id="zone" name="zone" value={formData.zone} onChange={handleChange}>
                  <option value="COLOMBO_SOUTH">Colombo South (04 - 06)</option>
                  <option value="COLOMBO_CENTRAL">Colombo Central (01 - 03)</option>
                  <option value="COLOMBO_NORTH">Colombo North (12 - 15)</option>
                  <option value="INTER_CITY">Inter-city (Galle, Kandy, etc.)</option>
                </select>
              </div>
            </div>

            <div className="reg-form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="address">Registered Address / Primary Warehouse *</label>
              <input
                id="address"
                name="address"
                type="text"
                required
                value={formData.address}
                onChange={handleChange}
                placeholder="e.g. No. 42, Ward Place, Colombo 07"
              />
            </div>
          </div>

          {/* Card 2: Contact Person */}
          <div className="reg-card">
            <h2 className="reg-card-title">2. Contact person</h2>

            <div className="reg-grid-2">
              <div className="reg-form-group">
                <label htmlFor="contactName">Full Name *</label>
                <input
                  id="contactName"
                  name="contactName"
                  type="text"
                  required
                  value={formData.contactName}
                  onChange={handleChange}
                  placeholder="e.g. Ayesha Rodrigo"
                />
              </div>

              <div className="reg-form-group">
                <label htmlFor="phone">Mobile Number *</label>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  required
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="+94 77 123 4567"
                />
                <p className="reg-hint">We send SMS verification and order alerts here.</p>
              </div>
            </div>

            <div className="reg-form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="email">Work Email *</label>
              <input
                id="email"
                name="email"
                type="email"
                required
                value={formData.email}
                onChange={handleChange}
                placeholder="e.g. ayesha@lotusleaf.lk"
              />
            </div>
          </div>

          {/* Card 3: Documents */}
          <div className="reg-card">
            <h2 className="reg-card-title">3. Verification documents</h2>
            <p style={{ fontSize: '12px', color: 'var(--muted)', marginTop: '-10px', marginBottom: '18px' }}>
              Upload clear scans or photos for identity and business verification.
            </p>

            <div className="reg-grid-2">
              <div
                className={`doc-upload-box ${hasBrDoc ? 'is-uploaded' : ''}`}
                onClick={() => setHasBrDoc(!hasBrDoc)}
              >
                <div style={{ color: hasBrDoc ? '#167a49' : 'var(--muted)', marginBottom: '8px' }}>
                  {hasBrDoc ? '✓' : '📄'}
                </div>
                <strong style={{ fontSize: '12px', display: 'block' }}>
                  {hasBrDoc ? 'br-certificate.pdf' : 'Business Registration Certificate'}
                </strong>
                <span style={{ fontSize: '10px', color: 'var(--muted)' }}>
                  {hasBrDoc ? '2.4 MB · Ready to upload' : 'PDF or JPG up to 10MB'}
                </span>
              </div>

              <div
                className={`doc-upload-box ${hasNicDoc ? 'is-uploaded' : ''}`}
                onClick={() => setHasNicDoc(!hasNicDoc)}
              >
                <div style={{ color: hasNicDoc ? '#167a49' : 'var(--muted)', marginBottom: '8px' }}>
                  {hasNicDoc ? '✓' : '🪪'}
                </div>
                <strong style={{ fontSize: '12px', display: 'block' }}>
                  {hasNicDoc ? 'nic-front-back.pdf' : 'Owner NIC / Passport'}
                </strong>
                <span style={{ fontSize: '10px', color: 'var(--muted)' }}>
                  {hasNicDoc ? '1.8 MB · Ready to upload' : 'Front & back scan · Up to 10MB'}
                </span>
              </div>
            </div>
          </div>

          {/* Terms & Actions */}
          <div style={{ marginBottom: '24px' }}>
            <label className="terms-check" style={{ cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={agreedTerms}
                onChange={(e) => setAgreedTerms(e.target.checked)}
              />
              <span style={{ fontSize: '12px', color: '#48566c' }}>
                I confirm this business is legally registered in Sri Lanka and agree to the <strong>SwiftDrop Merchant Terms & Conditions</strong>.
              </span>
            </label>
          </div>

          <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap' }}>
            <button
              type="submit"
              disabled={loading || !agreedTerms}
              className="button button-primary"
              style={{ flex: 1, minWidth: '200px' }}
            >
              {loading ? 'Submitting…' : 'Submit for approval'}
            </button>
            <button
              type="button"
              onClick={fillSampleData}
              className="button button-outline"
              style={{ width: 'auto', padding: '0 20px' }}
            >
              Fill with sample business
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
