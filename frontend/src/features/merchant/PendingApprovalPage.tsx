import React, { useEffect, useState } from 'react';
import { merchantApi, MerchantResponse } from '../../api/merchantApi';

interface Props {
  token: string;
}

export const PendingApprovalPage: React.FC<Props> = ({ token }) => {
  const [profile, setProfile] = useState<MerchantResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStatus = async () => {
    try {
      setLoading(true);
      const res = await merchantApi.getMyProfile(token);
      setProfile(res);
    } catch (err: any) {
      setError(err.message || 'Unable to fetch status');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
  }, [token]);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="max-w-md mx-auto my-12 p-6 bg-white dark:bg-gray-800 rounded-xl shadow text-center">
        <h3 className="text-lg font-semibold text-red-600 mb-2">Error Loading Status</h3>
        <p className="text-gray-600 dark:text-gray-300 mb-4">{error || 'No merchant registration found.'}</p>
        <button
          onClick={fetchStatus}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          Try Again
        </button>
      </div>
    );
  }

  const isApproved = profile.status === 'APPROVED';
  const isRejected = profile.status === 'REJECTED';

  return (
    <div className="max-w-xl mx-auto my-12 p-8 bg-white dark:bg-gray-800 rounded-xl shadow-lg text-center">
      <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 ${
        isApproved ? 'bg-green-100 text-green-600' : isRejected ? 'bg-red-100 text-red-600' : 'bg-yellow-100 text-yellow-600'
      }`}>
        {isApproved ? (
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
        ) : isRejected ? (
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        ) : (
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        )}
      </div>

      <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
        {isApproved ? 'Account Approved!' : isRejected ? 'Application Rejected' : 'Under Review'}
      </h2>

      <p className="text-gray-600 dark:text-gray-300 mb-6">
        {isApproved
          ? `Welcome ${profile.businessName}! Your account is approved. You can now access your dashboard and generate API keys.`
          : isRejected
          ? `Your application was rejected. Reason: ${profile.rejectionReason || 'Contact support for details.'}`
          : `Your registration for ${profile.businessName} (BRN: ${profile.businessRegNo}) is being verified by the operations team.`}
      </p>

      {isApproved ? (
        <a
          href="/merchant/dashboard"
          className="inline-block px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          Go to Dashboard
        </a>
      ) : (
        <button
          onClick={fetchStatus}
          className="px-5 py-2 border border-gray-300 rounded-lg text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
        >
          Refresh Status
        </button>
      )}
    </div>
  );
};
