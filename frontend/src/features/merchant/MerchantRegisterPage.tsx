import React, { useState } from 'react';
import { merchantApi, RegisterMerchantRequest } from '../../api/merchantApi';

export const MerchantRegisterPage: React.FC = () => {
  const [formData, setFormData] = useState<RegisterMerchantRequest>({
    businessName: '',
    businessRegNo: '',
    contactName: '',
    email: '',
    phone: '',
    address: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await merchantApi.register(formData);
      setSubmitted(true);
    } catch (err: any) {
      setError(err.message || 'Failed to submit registration');
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="max-w-xl mx-auto my-12 p-8 bg-white dark:bg-gray-800 rounded-xl shadow-lg text-center">
        <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Registration Submitted!</h2>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Your merchant application is currently pending review by our administrator team. You will be notified once approved.
        </p>
        <a
          href="/merchant/status"
          className="inline-block px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          Check Approval Status
        </a>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto my-8 p-6 bg-white dark:bg-gray-800 rounded-xl shadow">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Register as a Merchant</h1>
      <p className="text-sm text-gray-500 mb-6">
        Deliver parcels with SwiftDrop. Enter your business details for verification.
      </p>

      {error && (
        <div className="mb-4 p-4 text-sm text-red-700 bg-red-100 rounded-lg">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Business Name *
          </label>
          <input
            type="text"
            name="businessName"
            required
            value={formData.businessName}
            onChange={handleChange}
            placeholder="e.g. Acme Retailers (Pvt) Ltd"
            className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Business Registration Number (BRN) *
          </label>
          <input
            type="text"
            name="businessRegNo"
            required
            value={formData.businessRegNo}
            onChange={handleChange}
            placeholder="e.g. PV-123456"
            className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Contact Person Name *
          </label>
          <input
            type="text"
            name="contactName"
            required
            value={formData.contactName}
            onChange={handleChange}
            placeholder="e.g. Nimal Perera"
            className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Business Email *
            </label>
            <input
              type="email"
              name="email"
              required
              value={formData.email}
              onChange={handleChange}
              placeholder="orders@acme.lk"
              className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Phone Number *
            </label>
            <input
              type="tel"
              name="phone"
              required
              value={formData.phone}
              onChange={handleChange}
              placeholder="+94 77 123 4567"
              className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            Registered Address / Warehouse Address *
          </label>
          <textarea
            name="address"
            rows={3}
            required
            value={formData.address}
            onChange={handleChange}
            placeholder="No. 123, Galle Road, Colombo 03"
            className="w-full px-3 py-2 border rounded-lg dark:bg-gray-700 dark:text-white"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
        >
          {loading ? 'Submitting Application...' : 'Submit Application'}
        </button>
      </form>
    </div>
  );
};
