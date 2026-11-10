import React, { useEffect, useState } from 'react';
import { merchantApi, WebhookSettingsResponse, WebhookCreatedResponse } from '../../api/merchantApi';

interface Props {
  token: string;
}

export const WebhookSettingsPage: React.FC<Props> = ({ token }) => {
  const [, setSettings] = useState<WebhookSettingsResponse | null>(null);
  const [webhookUrl, setWebhookUrl] = useState('');
  const [rotateSecret, setRotateSecret] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedSecret, setSavedSecret] = useState<string | null>(null);

  const fetchSettings = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await merchantApi.getWebhookSettings(token);
      setSettings(data);
      setWebhookUrl(data.webhookUrl || '');
    } catch {
      setSettings(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSettings();
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!webhookUrl.trim()) return;

    setSaving(true);
    setError(null);
    setSavedSecret(null);

    try {
      const res: WebhookCreatedResponse = await merchantApi.updateWebhookSettings(token, webhookUrl.trim(), rotateSecret);
      setSavedSecret(res.secret);
      setRotateSecret(false);
      fetchSettings();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to save webhook settings';
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="merchant-body" style={{ maxWidth: '840px' }}>
      <div className="merchant-page-head">
        <div>
          <h1>Webhook Settings</h1>
          <p>
            Receive real-time notifications whenever parcels change status.
          </p>
        </div>
      </div>

      {error && (
        <div className="alert" role="alert" style={{ marginBottom: '20px' }}>
          {error}
        </div>
      )}

      {savedSecret && (
        <div className="demo-callout-banner" style={{ marginBottom: '24px', background: '#e6f6ee', borderColor: '#a7dfb8' }}>
          <strong style={{ fontSize: '13px', color: '#167a49', display: 'block', marginBottom: '6px' }}>
            ✓ Webhook Secret Generated
          </strong>
          <p style={{ fontSize: '11px', color: '#14603b', margin: '0 0 12px' }}>
            Use this secret to verify the <code>X-SwiftDrop-Signature</code> header on inbound webhook requests.
          </p>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <input
              type="text"
              readOnly
              value={savedSecret}
              style={{
                fontFamily: 'monospace',
                fontSize: '12px',
                padding: '8px 12px',
                borderRadius: '8px',
                border: '1px solid #a7dfb8',
                width: '100%',
                maxWidth: '420px',
                background: '#ffffff',
              }}
            />
            <button
              type="button"
              onClick={() => navigator.clipboard.writeText(savedSecret)}
              className="button button-primary"
              style={{ width: 'auto', padding: '0 18px', height: '36px', fontSize: '11px' }}
            >
              Copy Secret
            </button>
          </div>
        </div>
      )}

      <div className="dev-card">
        {loading ? (
          <p style={{ color: 'var(--muted)', textAlign: 'center', padding: '20px' }}>Loading webhook settings…</p>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="reg-form-group">
              <label htmlFor="webhookUrl">Webhook Endpoint URL *</label>
              <input
                id="webhookUrl"
                type="url"
                required
                value={webhookUrl}
                onChange={(e) => setWebhookUrl(e.target.value)}
                placeholder="https://api.yourdomain.com/swiftdrop/webhook"
              />
              <p className="reg-hint">Must accept HTTPS POST requests with JSON payloads.</p>
            </div>

            <div style={{ margin: '20px 0' }}>
              <label className="terms-check" style={{ cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={rotateSecret}
                  onChange={(e) => setRotateSecret(e.target.checked)}
                />
                <span style={{ fontSize: '12px', color: '#48566c' }}>
                  Rotate signing secret (invalidates previously issued secret immediately)
                </span>
              </label>
            </div>

            <button
              type="submit"
              disabled={saving}
              className="button button-primary"
              style={{ width: 'auto', padding: '0 24px', height: '40px' }}
            >
              {saving ? 'Saving…' : 'Save webhook configuration'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
