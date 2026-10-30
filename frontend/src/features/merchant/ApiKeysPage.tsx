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
    } catch (err: any) {
      setError(err.message || 'Failed to load API keys');
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
    } catch (err: any) {
      setError(err.message || 'Failed to generate API key');
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
    } catch (err: any) {
      setError(err.message || 'Failed to revoke API key');
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
    <div className="max-w-5xl mx-auto p-4 sm:p-6 lg:p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Merchant API Keys</h1>
        <p className="text-sm text-gray-500">
          Authenticate external e-commerce systems with the <code className="bg-gray-100 dark:bg-gray-700 px-1.5 py-0.5 rounded">X-API-Key</code> header.
        </p>
      </div>

      {error && (
        <div className="mb-4 p-4 text-sm text-red-700 bg-red-100 rounded-lg">
          {error}
        </div>
      )}

      {/* One-time Key Creation Notification Modal */}
      {newKeyData && (
        <div className="mb-6 p-6 bg-amber-50 dark:bg-amber-950 border border-amber-300 dark:border-amber-700 rounded-xl shadow-md">
          <div className="flex items-start">
            <div className="flex-shrink-0 text-amber-500">
              <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div className="ml-3 flex-1">
              <h3 className="text-base font-semibold text-amber-900 dark:text-amber-200">
                Save Your API Key Now
              </h3>
              <p className="text-sm text-amber-800 dark:text-amber-300 mt-1">
                This is the only time your raw API key will be displayed. It cannot be retrieved again after closing this dialog.
              </p>
              <div className="mt-3 flex items-center gap-2">
                <input
                  type="text"
                  readOnly
                  value={newKeyData.rawApiKey}
                  className="font-mono text-sm px-3 py-2 bg-white dark:bg-gray-900 border border-amber-300 rounded-lg w-full max-w-md select-all"
                />
                <button
                  onClick={handleCopy}
                  className="px-4 py-2 bg-amber-600 text-white rounded-lg text-sm font-medium hover:bg-amber-700 transition"
                >
                  {copied ? 'Copied!' : 'Copy Key'}
                </button>
                <button
                  onClick={() => setNewKeyData(null)}
                  className="px-3 py-2 text-gray-500 hover:text-gray-700 text-sm"
                >
                  Dismiss
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Create Key Form */}
      <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow mb-8">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Create New API Key</h2>
        <form onSubmit={handleCreate} className="flex flex-col sm:flex-row gap-3">
          <input
            type="text"
            required
            value={keyName}
            onChange={(e) => setKeyName(e.target.value)}
            placeholder="Key Description (e.g., Shopify Store, ERP System)"
            className="flex-1 px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
          />
          <button
            type="submit"
            disabled={creating}
            className="px-6 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
          >
            {creating ? 'Generating...' : 'Generate Key'}
          </button>
        </form>
      </div>

      {/* Keys Table */}
      <div className="bg-white dark:bg-gray-800 rounded-xl shadow overflow-hidden">
        <div className="p-4 border-b dark:border-gray-700">
          <h2 className="text-base font-semibold text-gray-900 dark:text-white">Active & Revoked Keys</h2>
        </div>

        {loading ? (
          <div className="p-8 text-center text-gray-500">Loading keys...</div>
        ) : keys.length === 0 ? (
          <div className="p-8 text-center text-gray-500">No API keys found. Generate one above to start integration.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-200 text-xs uppercase">
                <tr>
                  <th className="px-6 py-3">Name</th>
                  <th className="px-6 py-3">Prefix</th>
                  <th className="px-6 py-3">Created At</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {keys.map((k) => (
                  <tr key={k.id} className="hover:bg-gray-50 dark:hover:bg-gray-750">
                    <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">{k.name}</td>
                    <td className="px-6 py-4 font-mono text-xs text-gray-600 dark:text-gray-300">
                      {k.keyPrefix}••••••••
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-500">
                      {new Date(k.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4">
                      {k.revoked ? (
                        <span className="px-2 py-0.5 text-xs bg-red-100 text-red-800 rounded-full font-medium">
                          Revoked
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 text-xs bg-green-100 text-green-800 rounded-full font-medium">
                          Active
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {!k.revoked && (
                        <button
                          onClick={() => handleRevoke(k.id)}
                          className="text-xs text-red-600 hover:text-red-800 font-medium"
                        >
                          Revoke
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
