import React, { useState } from 'react';
import { merchantApi, RegisterMerchantRequest } from '../../api/merchantApi';
import './merchant.css';

interface Props {
  token?: string;
  onBackToSignIn?: () => void;
  onRegistered?: (businessName: string, regNo: string) => void;
}

export const MerchantRegisterPage: React.FC<Props> = ({ token, onBackToSignIn, onRegistered }) => {
  const [formData, setFormData] = useState<RegisterMerchantRequest>({
    businessName: '',
    businessRegNo: '',
    contactName: '',
    email: '',
    phone: '',
    address: '',
  });
  const [agreedTerms, setAgreedTerms] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
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

  return (
    <div className="reg-shell">
      <div className="reg-container">
        {onBackToSignIn && (
          <button type="button" className="reg-back-link" onClick={onBackToSignIn}>
            ← Back to my deliveries
          </button>
        )}

        {/* Title Section */}
        <div className="reg-header">
          <p className="eyebrow" style={{ color: 'var(--orange-dark)', fontWeight: 800, fontSize: '10px', letterSpacing: '0.12em', margin: 0 }}>
            MERCHANT REGISTRATION
          </p>
          <h1>Register your business</h1>
          <p>Send your store's orders with SwiftDrop across Colombo and to Kandy, Galle and Negombo. A SwiftDrop admin reviews every application.</p>
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

            <div className="reg-form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="address">Pickup address *</label>
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
          </div>
        </form>
      </div>
    </div>
  );
};
