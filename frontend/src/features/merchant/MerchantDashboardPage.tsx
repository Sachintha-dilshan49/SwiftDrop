import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantOrderResponse } from '../../api/merchantApi';

interface Props {
  token: string;
}

export const MerchantDashboardPage: React.FC<Props> = ({ token }) => {
  const [orders, setOrders] = useState<MerchantOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchOrders = async (pageNo: number) => {
    try {
      setLoading(true);
      setError(null);
      const res = await merchantApi.listOrders(token, pageNo, 15);
      setOrders(res.content || []);
      setTotalElements(res.totalElements || 0);
      setPage(pageNo);
    } catch (err: any) {
      setError(err.message || 'Failed to load merchant orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders(0);
  }, [token]);

  const getStatusBadge = (status: string) => {
    const base = 'px-2.5 py-1 text-xs font-semibold rounded-full';
    switch (status) {
      case 'DELIVERED':
        return `${base} bg-green-100 text-green-800`;
      case 'OUT_FOR_DELIVERY':
      case 'IN_TRANSIT':
      case 'PICKED_UP':
        return `${base} bg-blue-100 text-blue-800`;
      case 'FAILED':
        return `${base} bg-red-100 text-red-800`;
      default:
        return `${base} bg-gray-100 text-gray-800`;
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-4 sm:p-6 lg:p-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Merchant Order Dashboard</h1>
          <p className="text-sm text-gray-500">Track orders created via SwiftDrop API</p>
        </div>
        <div className="flex gap-2">
          <a
            href="/merchant/api-keys"
            className="px-4 py-2 border rounded-lg text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-700 transition"
          >
            Manage API Keys
          </a>
          <a
            href="/merchant/webhooks"
            className="px-4 py-2 border rounded-lg text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-700 transition"
          >
            Webhook Settings
          </a>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 text-sm text-red-700 bg-red-100 rounded-lg">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-gray-800 rounded-xl shadow border border-gray-100 dark:border-gray-700">
          <svg className="mx-auto h-12 w-12 text-gray-400 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
          <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-1">No orders created yet</h3>
          <p className="text-sm text-gray-500 mb-4">
            Integrate your e-commerce store with SwiftDrop by generating an API key.
          </p>
          <a
            href="/merchant/api-keys"
            className="inline-block px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 transition"
          >
            Create Your First API Key
          </a>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-200 uppercase text-xs">
                <tr>
                  <th className="px-6 py-3">Tracking No</th>
                  <th className="px-6 py-3">Recipient</th>
                  <th className="px-6 py-3">Destination</th>
                  <th className="px-6 py-3">Fee</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Created At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {orders.map((o) => (
                  <tr key={o.id} className="hover:bg-gray-50 dark:hover:bg-gray-750">
                    <td className="px-6 py-4 font-mono font-medium text-blue-600 dark:text-blue-400">
                      <a href={`/track/${o.trackingNo}`} className="hover:underline">
                        {o.trackingNo}
                      </a>
                    </td>
                    <td className="px-6 py-4">
                      <div className="font-medium text-gray-900 dark:text-white">{o.recipientName}</div>
                      <div className="text-xs text-gray-500">{o.recipientPhone}</div>
                    </td>
                    <td className="px-6 py-4 max-w-xs truncate text-gray-600 dark:text-gray-300">
                      {o.dropoffAddress}
                    </td>
                    <td className="px-6 py-4 font-medium text-gray-900 dark:text-white">
                      {o.feeCurrency} {Number(o.feeAmount).toFixed(2)}
                    </td>
                    <td className="px-6 py-4">
                      <span className={getStatusBadge(o.status)}>{o.status}</span>
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-500">
                      {new Date(o.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-4 border-t border-gray-200 dark:border-gray-700 flex justify-between items-center text-sm">
            <span className="text-gray-500">Total Orders: {totalElements}</span>
            <div className="space-x-2">
              <button
                disabled={page === 0}
                onClick={() => fetchOrders(page - 1)}
                className="px-3 py-1 border rounded disabled:opacity-40"
              >
                Previous
              </button>
              <button
                disabled={(page + 1) * 15 >= totalElements}
                onClick={() => fetchOrders(page + 1)}
                className="px-3 py-1 border rounded disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
