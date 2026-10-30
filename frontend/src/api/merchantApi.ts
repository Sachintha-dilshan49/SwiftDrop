// frontend/src/api/merchantApi.ts

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

export interface RegisterMerchantRequest {
  businessName: string;
  businessRegNo: string;
  contactName: string;
  email: string;
  phone: string;
  address: string;
}

export interface MerchantResponse {
  id: string;
  userId?: string;
  businessName: string;
  businessRegNo: string;
  contactName: string;
  email: string;
  phone: string;
  address: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ApiKeyResponse {
  id: string;
  name: string;
  keyPrefix: string;
  revoked: boolean;
  revokedAt?: string;
  expiresAt?: string;
  createdAt: string;
}

export interface ApiKeyCreatedResponse extends ApiKeyResponse {
  rawApiKey: string;
}

export interface MerchantOrderResponse {
  id: string;
  parcelId: string;
  trackingNo: string;
  recipientName: string;
  recipientPhone: string;
  dropoffAddress: string;
  status: string;
  feeAmount: number;
  feeCurrency: string;
  createdAt: string;
}

export interface WebhookSettingsResponse {
  id: string;
  webhookUrl: string;
  active: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface WebhookCreatedResponse {
  id: string;
  webhookUrl: string;
  secret: string;
  active: boolean;
  createdAt: string;
}

async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ detail: response.statusText }));
    throw new Error(errorData.detail || errorData.title || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return null as T;
  }

  return response.json();
}

export const merchantApi = {
  // Merchant Registration & Profile
  register: (data: RegisterMerchantRequest, token?: string) =>
    request<MerchantResponse>('/v1/merchants/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }, token),

  getMyProfile: (token: string) =>
    request<MerchantResponse>('/v1/merchants/me', { method: 'GET' }, token),

  // Admin Approval
  listPendingMerchants: (token: string, status = 'PENDING', page = 0, size = 20) =>
    request<{ content: MerchantResponse[]; totalElements: number }>(
      `/v1/admin/merchants?status=${status}&page=${page}&size=${size}`,
      { method: 'GET' },
      token
    ),

  approveMerchant: (token: string, id: string) =>
    request<MerchantResponse>(`/v1/admin/merchants/${id}/approve`, { method: 'PATCH' }, token),

  rejectMerchant: (token: string, id: string, reason: string) =>
    request<MerchantResponse>(`/v1/admin/merchants/${id}/reject`, {
      method: 'PATCH',
      body: JSON.stringify({ reason }),
    }, token),

  // API Keys
  createApiKey: (token: string, name: string, expiresAt?: string) =>
    request<ApiKeyCreatedResponse>('/v1/merchants/me/api-keys', {
      method: 'POST',
      body: JSON.stringify({ name, expiresAt }),
    }, token),

  listApiKeys: (token: string) =>
    request<ApiKeyResponse[]>('/v1/merchants/me/api-keys', { method: 'GET' }, token),

  revokeApiKey: (token: string, id: string) =>
    request<void>(`/v1/merchants/me/api-keys/${id}`, { method: 'DELETE' }, token),

  // Orders Dashboard
  listOrders: (token: string, page = 0, size = 20) =>
    request<{ content: MerchantOrderResponse[]; totalElements: number }>(
      `/v1/merchants/me/orders?page=${page}&size=${size}`,
      { method: 'GET' },
      token
    ),

  getOrderById: (token: string, id: string) =>
    request<MerchantOrderResponse>(`/v1/merchants/me/orders/${id}`, { method: 'GET' }, token),

  // Webhooks
  getWebhookSettings: (token: string) =>
    request<WebhookSettingsResponse>('/v1/merchants/me/webhook', { method: 'GET' }, token),

  updateWebhookSettings: (token: string, webhookUrl: string, rotateSecret = false) =>
    request<WebhookCreatedResponse>('/v1/merchants/me/webhook', {
      method: 'PUT',
      body: JSON.stringify({ webhookUrl, rotateSecret }),
    }, token),
};
