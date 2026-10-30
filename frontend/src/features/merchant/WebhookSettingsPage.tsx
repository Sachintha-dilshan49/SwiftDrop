import React, { useEffect, useState } from 'react';
import { merchantApi, WebhookSettingsResponse, WebhookCreatedResponse } from '../../api/merchantApi';

interface Props {
  token: string;
}

export const WebhookSettingsPage: React.FC<Props> = ({ token }) => {
  const [settings, setSettings] = useState<WebhookSettingsResponse | null>(null);
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
    } catch (err: any) {
      // 404 means not yet configured
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
    } catch (err: any) {
      setError(err.message || 'Failed to save webhook settings');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Webhook Settings</h1>
        <p className="text-sm text-gray-500">
          Receive real-time notifications whenever your parcels change status (delivered by SwiftDrop Notification Service).
        </p>
      </div>

      {error && (
        <div className="mb-4 p-4 text-sm text-red-700 bg-red-100 rounded-lg">
          {error}
        </div>
      )}

      {/* Secret Display Banner (shown only on save / rotation) */}
      {savedSecret && (
        <div className="mb-6 p-6 bg-green-50 border border-green-200 rounded-xl shadow-sm">
          <h3 className="text-sm font-semibold text-green-900 mb-1">Webhook Secret Generated</h3>
          <p className="text-xs text-green-800 mb-3">
            Use this secret to verify the <code className="font-mono bg-white px-1 py-0.5 rounded">X-SwiftDrop-Signature</code> header on inbound webhook POST requests. Copy it now:
          </p>
          <div className="flex items-center gap-2">
            <input
              type="text"
              readOnly
              value={savedSecret}
              className="font-mono text-sm px-3 py-2 bg-white border border-green-300 rounded-lg w-full max-w-md select-all"
            />
            <button
              onClick={() => navigator.clipboard.writeText(savedSecret)}
              className="px-3 py-2 bg-green-600 text-white text-xs rounded-lg font-medium hover:bg-green-700 transition"
            >
              Copy Secret
            </button>
          </div>
        </div>
      )}

      <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow mb-8">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Webhook Endpoint URL *
            </label>
            <input
              type="url"
              required
              value={webhookUrl}
              onChange={(e) => setWebhookUrl(e.target.value)}
              placeholder="https://your-domain.com/api/swiftdrop/webhook"
              className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white text-sm"
            />
            <span className="text-xs text-gray-500">
              Must be a valid HTTP or HTTPS endpoint that accepts JSON POST requests.
            </span>
          </div>

          {settings && (
            <div className="flex items-center gap-2 pt-2">
              <input
                type="checkbox"
                id="rotateSecret"
                checked={rotateSecret}
                onChange={(e) => setRotateSecret(e.target.checked)}
                className="rounded border-gray-300 text-blue-600"
              />
              <label htmlFor="rotateSecret" className="text-sm text-gray-700 dark:text-gray-300">
                Rotate HMAC-SHA256 signing secret on save
              </label>
            </div>
          )}

          <div className="pt-2">
            <button
              type="submit"
              disabled={saving}
              className="px-6 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
            >
              {saving ? 'Saving...' : settings ? 'Update Webhook' : 'Configure Webhook'}
            </button>
          </div>
        </form>
      </div>

      {/* Signature Verification Guide */}
      <div className="bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl p-6">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-2">Signature Verification Guide</h3>
        <p className="text-xs text-gray-600 dark:text-gray-400 mb-2">
          Every webhook payload sent by SwiftDrop includes the following security headers:
        </p>
        <ul className="text-xs text-gray-600 dark:text-gray-400 list-disc list-inside space-y-1 mb-3">
          <li><code className="font-mono">X-SwiftDrop-Signature</code>: sha256=&lt;HMAC-SHA256 signature using your secret&gt;</li>
          <li><code className="font-mono">X-SwiftDrop-Event-Id</code>: Unique event UUID for idempotency</li>
          <li><code className="font-mono">X-SwiftDrop-Timestamp</code>: UTC ISO-8601 timestamp of delivery</li>
        </ul>
      </div>
    </div>
  );
};
