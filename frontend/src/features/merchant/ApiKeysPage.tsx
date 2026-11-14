import React, { useEffect, useState } from 'react';
import { merchantApi, ApiKeyResponse, ApiKeyCreatedResponse } from '../../api/merchantApi';

interface Props {
  token: string;
}

export const ApiKeysPage: React.FC<Props> = ({ token }) => {
  const [keys, setKeys] = useState<ApiKeyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyName, setKeyName] = useState('');
  const [creating, setCreating] = useState(false);
  const [newKeyData, setNewKeyData] = useState<ApiKeyCreatedResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const fetchKeys = async () => {
    try {
      setLoading(true);
      const data = await merchantApi.listApiKeys(token);
      setKeys(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load API keys';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchKeys();
  }, [token]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!keyName.trim()) return;
    setCreating(true);
    setError(null);

    try {
      const res = await merchantApi.createApiKey(token, keyName.trim());
      setNewKeyData(res);
      setKeyName('');
      fetchKeys();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to generate API key';
      setError(message);
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (id: string) => {
    if (!window.confirm('Are you sure you want to revoke this API key? This action is permanent and cannot be undone.')) {
      return;
    }

    try {
      await merchantApi.revokeApiKey(token, id);
      fetchKeys();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to revoke API key';
      setError(message);
    }
  };

  const handleCopy = () => {
    if (newKeyData?.rawApiKey) {
      navigator.clipboard.writeText(newKeyData.rawApiKey);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="merchant-body" style={{ maxWidth: '960px' }}>
      <div className="merchant-page-head">
        <div>
          <h1>Merchant API Keys</h1>
          <p>
            Authenticate external systems with the <code>X-API-Key</code> header.
          </p>
        </div>
      </div>

      {error && (
        <div className="alert" role="alert" style={{ marginBottom: '20px' }}>
          {error}
        </div>
      )}

      {newKeyData && (
        <div className="demo-callout-banner" style={{ marginBottom: '24px', background: '#fffbeb', borderColor: '#fcd34d' }}>
          <strong style={{ fontSize: '13px', color: '#92400e', display: 'block', marginBottom: '6px' }}>
            ⚠️ Save Your API Key Now
          </strong>
          <p style={{ fontSize: '11px', color: '#78350f', margin: '0 0 12px' }}>
            This is the only time your raw API key will be displayed. It cannot be retrieved again after closing.
          </p>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <input
              type="text"
              readOnly
              value={newKeyData.rawApiKey}
              style={{
                fontFamily: 'monospace',
                fontSize: '12px',
                padding: '8px 12px',
                borderRadius: '8px',
                border: '1px solid #fcd34d',
                width: '100%',
                maxWidth: '420px',
                background: '#ffffff',
              }}
            />
            <button
              type="button"
              onClick={handleCopy}
              className="button button-primary"
              style={{ width: 'auto', padding: '0 18px', height: '36px', fontSize: '11px' }}
            >
              {copied ? '✓ Copied' : 'Copy Key'}
            </button>
            <button
              type="button"
              onClick={() => setNewKeyData(null)}
              className="button button-outline"
              style={{ width: 'auto', padding: '0 14px', height: '36px', fontSize: '11px' }}
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Create Key Card */}
      <div className="dev-card" style={{ marginBottom: '24px' }}>
        <h2 className="dev-card-title" style={{ marginBottom: '14px' }}>Generate new API key</h2>
        <form onSubmit={handleCreate} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <input
            type="text"
            required
            value={keyName}
            onChange={(e) => setKeyName(e.target.value)}
            placeholder="Key Description (e.g. Shopify Store, Warehouse ERP)"
            style={{
              flex: 1,
              minWidth: '240px',
              height: '40px',
              padding: '0 12px',
              borderRadius: '8px',
              border: '1px solid var(--line)',
            }}
          />
          <button
            type="submit"
            disabled={creating}
            className="button button-primary"
            style={{ width: 'auto', padding: '0 20px', height: '40px' }}
          >
            {creating ? 'Generating…' : 'Generate Key'}
          </button>
        </form>
      </div>

      {/* Keys Table */}
      <div className="merchant-card-box">
        <table className="merchant-table">
          <thead>
            <tr>
              <th>Key Name</th>
              <th>Prefix</th>
              <th>Created</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>Loading API keys…</td>
              </tr>
            ) : keys.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '24px', color: 'var(--muted)' }}>
                  No API keys generated yet.
                </td>
              </tr>
            ) : (
              keys.map((k) => (
                <tr key={k.id}>
                  <td><strong>{k.name}</strong></td>
                  <td><code>{k.keyPrefix}••••••••</code></td>
                  <td style={{ color: 'var(--muted)' }}>{new Date(k.createdAt).toLocaleDateString()}</td>
                  <td>
                    {k.revoked ? (
                      <span className="status-pill failed">Revoked</span>
                    ) : (
                      <span className="status-pill delivered">Active</span>
                    )}
                  </td>
                  <td>
                    {!k.revoked && (
                      <button
                        type="button"
                        onClick={() => handleRevoke(k.id)}
                        style={{ background: 'none', border: 'none', color: '#b32a2a', fontWeight: 600, fontSize: '11px', cursor: 'pointer' }}
                      >
                        Revoke
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
