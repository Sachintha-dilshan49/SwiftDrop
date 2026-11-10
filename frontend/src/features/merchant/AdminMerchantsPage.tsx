import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantResponse } from '../../api/merchantApi';

interface Props {
  adminToken: string;
}

export const AdminMerchantsPage: React.FC<Props> = ({ adminToken }) => {
  const [merchants, setMerchants] = useState<MerchantResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING');
  const [error, setError] = useState<string | null>(null);
  const [selectedMerchant, setSelectedMerchant] = useState<MerchantResponse | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const fetchMerchants = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await merchantApi.listPendingMerchants(adminToken, statusFilter);
      setMerchants(res.content || []);
    } catch (err: any) {
      setError(err.message || 'Failed to load merchants');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMerchants();
  }, [adminToken, statusFilter]);

  const handleApprove = async (id: string) => {
    if (!window.confirm('Approve this merchant account?')) return;
    try {
      setActionLoading(true);
      await merchantApi.approveMerchant(adminToken, id);
      fetchMerchants();
    } catch (err: any) {
      alert(err.message || 'Approval failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMerchant || !rejectReason.trim()) return;

    try {
      setActionLoading(true);
      await merchantApi.rejectMerchant(adminToken, selectedMerchant.id, rejectReason.trim());
      setSelectedMerchant(null);
      setRejectReason('');
      fetchMerchants();
    } catch (err: any) {
      alert(err.message || 'Rejection failed');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-4 sm:p-6 lg:p-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Merchant Registrations (Admin)</h1>
          <p className="text-sm text-gray-500">Review, approve, or reject business partner applications</p>
        </div>
        <div className="flex gap-2">
          {(['PENDING', 'APPROVED', 'REJECTED'] as const).map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition ${
                statusFilter === s
                  ? 'bg-blue-600 text-white'
                  : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 text-sm text-red-700 bg-red-100 rounded-lg">
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-20 text-center text-gray-500">Loading registrations...</div>
      ) : merchants.length === 0 ? (
        <div className="py-16 text-center bg-white dark:bg-gray-800 rounded-xl shadow text-gray-500">
          No {statusFilter.toLowerCase()} merchant registrations found.
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-200 text-xs uppercase">
                <tr>
                  <th className="px-6 py-3">Business Details</th>
                  <th className="px-6 py-3">BRN</th>
                  <th className="px-6 py-3">Contact</th>
                  <th className="px-6 py-3">Address</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {merchants.map((m) => (
                  <tr key={m.id} className="hover:bg-gray-50 dark:hover:bg-gray-750">
                    <td className="px-6 py-4">
                      <div className="font-semibold text-gray-900 dark:text-white">{m.businessName}</div>
                      <div className="text-xs text-gray-500">{m.email}</div>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-gray-600 dark:text-gray-300">
                      {m.businessRegNo}
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-gray-900 dark:text-white">{m.contactName}</div>
                      <div className="text-xs text-gray-500">{m.phone}</div>
                    </td>
                    <td className="px-6 py-4 text-xs max-w-xs truncate text-gray-600 dark:text-gray-300">
                      {m.address}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${
                        m.status === 'APPROVED' ? 'bg-green-100 text-green-800' :
                        m.status === 'REJECTED' ? 'bg-red-100 text-red-800' :
                        'bg-yellow-100 text-yellow-800'
                      }`}>
                        {m.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      {m.status === 'PENDING' && (
                        <>
                          <button
                            disabled={actionLoading}
                            onClick={() => handleApprove(m.id)}
                            className="px-3 py-1 bg-green-600 text-white rounded text-xs font-medium hover:bg-green-700 disabled:opacity-50"
                          >
                            Approve
                          </button>
                          <button
                            disabled={actionLoading}
                            onClick={() => setSelectedMerchant(m)}
                            className="px-3 py-1 bg-red-600 text-white rounded text-xs font-medium hover:bg-red-700 disabled:opacity-50"
                          >
                            Reject
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Reject Reason Modal */}
      {selectedMerchant && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white dark:bg-gray-800 rounded-xl max-w-md w-full p-6 shadow-xl">
            <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">
              Reject Registration: {selectedMerchant.businessName}
            </h3>
            <p className="text-sm text-gray-500 mb-4">
              Please enter the reason for rejecting this application. This will be visible to the merchant.
            </p>
            <form onSubmit={handleRejectSubmit}>
              <textarea
                required
                rows={3}
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="e.g. Invalid business registration document provided."
                className="w-full px-3 py-2 border rounded-lg mb-4 dark:bg-gray-700 dark:text-white text-sm"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedMerchant(null)}
                  className="px-4 py-2 border rounded-lg text-sm text-gray-700 dark:text-gray-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50"
                >
                  {actionLoading ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
