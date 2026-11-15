import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { MerchantOrderResponse, MerchantResponse } from '../../api/merchantApi';
import { AdminMerchantsPage } from './AdminMerchantsPage';
import { ApiKeysPage } from './ApiKeysPage';
import { MerchantDashboardPage } from './MerchantDashboardPage';
import { MerchantRegisterPage } from './MerchantRegisterPage';
import { orderStats } from './merchantStats';
import { PendingApprovalPage } from './PendingApprovalPage';

const MERCHANT: MerchantResponse = {
  id: 'm-1',
  businessName: 'Kandy Spice House',
  businessRegNo: 'PV123456',
  contactName: 'Ruwan Fernando',
  email: 'ruwan@shop.example.com',
  phone: '0771234567',
  address: '12 Peradeniya Road, Kandy',
  status: 'APPROVED',
  createdAt: '2026-10-01T04:00:00Z',
  updatedAt: '2026-10-02T04:00:00Z',
};

function order(id: string, status: string, createdAt: string): MerchantOrderResponse {
  return {
    id, parcelId: `p-${id}`, trackingNo: `SD-AAAA-${id}`, recipientName: `Recipient ${id}`, recipientPhone: '0770000000',
    dropoffAddress: '5 Station Road, Nugegoda', status, feeAmount: 450, feeCurrency: 'LKR', createdAt,
  };
}

function json(status: number, body: unknown): Response {
  return new Response(body === null ? null : JSON.stringify(body), {
    status, headers: { 'Content-Type': 'application/json' },
  });
}

/** Answers fetch by URL; each call gets a fresh Response. */
function stubApi(routes: Record<string, (init?: RequestInit) => Response>) {
  const fetchMock = vi.fn((url: string, init?: RequestInit) => {
    const match = Object.keys(routes).find((path) => String(url).includes(path));
    return Promise.resolve(match ? routes[match](init) : json(404, { detail: 'Not found' }));
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('MerchantRegisterPage', () => {
  it('sends only what the API stores, with the login token', async () => {
    const fetchMock = stubApi({ '/v1/merchants/register': () => json(201, { ...MERCHANT, status: 'PENDING' }) });
    const onRegistered = vi.fn();
    render(<MerchantRegisterPage token="t-1" onRegistered={onRegistered} />);

    await userEvent.type(screen.getByLabelText(/Business Name/), 'Kandy Spice House');
    await userEvent.type(screen.getByLabelText(/Business Registration Number/), 'PV123456');
    await userEvent.type(screen.getByLabelText(/Pickup address/), '12 Peradeniya Road, Kandy');
    await userEvent.type(screen.getByLabelText(/Full Name/), 'Ruwan Fernando');
    await userEvent.type(screen.getByLabelText(/Mobile Number/), '0771234567');
    await userEvent.type(screen.getByLabelText(/Work Email/), 'ruwan@shop.example.com');
    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('button', { name: 'Submit for approval' }));

    await waitFor(() => expect(onRegistered).toHaveBeenCalled());
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(init.body as string)).toEqual({
      businessName: 'Kandy Spice House', businessRegNo: 'PV123456', contactName: 'Ruwan Fernando',
      email: 'ruwan@shop.example.com', phone: '0771234567', address: '12 Peradeniya Road, Kandy',
    });
    expect(init.headers).toMatchObject({ Authorization: 'Bearer t-1' });
  });

  it('has no sample data, fake document uploads or unsupported fields', () => {
    render(<MerchantRegisterPage token="t" />);

    expect(screen.queryByRole('button', { name: /sample/i })).not.toBeInTheDocument();
    expect(screen.queryByText(/Verification documents/)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Category/)).not.toBeInTheDocument();
    expect(screen.queryByText(/COD/)).not.toBeInTheDocument();
  });
});

describe('PendingApprovalPage', () => {
  it('shows the real application while it waits for review', async () => {
    stubApi({ '/v1/merchants/me': () => json(200, { ...MERCHANT, status: 'PENDING' }) });
    render(<PendingApprovalPage token="t" />);

    expect(await screen.findByRole('heading', { name: 'Your application is being reviewed' })).toBeInTheDocument();
    expect(screen.getByText('Kandy Spice House')).toBeInTheDocument();
    expect(screen.getByText('PV123456')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Check again' })).toBeInTheDocument();
  });

  it('opens the portal once approved', async () => {
    stubApi({ '/v1/merchants/me': () => json(200, MERCHANT) });
    const onOpenPortal = vi.fn().mockResolvedValue(undefined);
    render(<PendingApprovalPage token="t" onOpenPortal={onOpenPortal} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Open the merchant portal' }));

    expect(onOpenPortal).toHaveBeenCalled();
  });

  it('shows the rejection reason', async () => {
    stubApi({ '/v1/merchants/me': () => json(200, { ...MERCHANT, status: 'REJECTED', rejectionReason: 'BRN not found' }) });
    render(<PendingApprovalPage token="t" />);

    expect(await screen.findByText(/BRN not found/)).toBeInTheDocument();
  });

  it('offers to apply when this account has no application', async () => {
    stubApi({ '/v1/merchants/me': () => json(404, { detail: 'Merchant profile not found for user: u' }) });
    const onRegister = vi.fn();
    render(<PendingApprovalPage token="t" onRegister={onRegister} />);

    await userEvent.click(await screen.findByRole('button', { name: 'Apply as a merchant' }));

    expect(onRegister).toHaveBeenCalled();
    expect(screen.queryByText(/Lotus Leaf/)).not.toBeInTheDocument();
  });
});

describe('MerchantDashboardPage', () => {
  const orders = [
    order('0001', 'DELIVERED', '2026-11-10T05:00:00Z'),
    order('0002', 'IN_TRANSIT', '2026-11-13T05:00:00Z'),
    order('0003', 'FAILED', '2026-11-12T05:00:00Z'),
    order('0004', 'CREATED', '2026-11-13T06:00:00Z'),
  ];

  it('counts from the real orders and filters them', async () => {
    stubApi({
      '/v1/merchants/me/orders': () => json(200, { content: orders, totalElements: 4 }),
      '/v1/merchants/me': () => json(200, MERCHANT),
    });
    render(<MerchantDashboardPage token="t" />);

    expect(await screen.findByText('Kandy Spice House')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'All 4' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Live 2' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Failed 1' }));

    const table = screen.getByRole('table');
    expect(within(table).getByText('SD-AAAA-0003')).toBeInTheDocument();
    expect(within(table).queryByText('SD-AAAA-0001')).not.toBeInTheDocument();
  });

  it('shows an error instead of sample orders when loading fails', async () => {
    stubApi({ '/v1/merchants/me': () => json(500, { detail: 'Something broke' }) });
    render(<MerchantDashboardPage token="t" />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Something broke');
    expect(screen.queryByText(/SD-20479|Ayesha|Lotus Leaf/)).not.toBeInTheDocument();
  });

  it('searches orders and shows the real API keys and webhook in developer settings', async () => {
    stubApi({
      '/v1/merchants/me/orders': () => json(200, { content: orders, totalElements: 4 }),
      '/v1/merchants/me/api-keys': () => json(200, []),
      '/v1/merchants/me/webhook': () => json(404, { detail: 'No webhook configured' }),
      '/v1/merchants/me': () => json(200, MERCHANT),
    });
    render(<MerchantDashboardPage token="t" />);

    await userEvent.click(await screen.findByRole('tab', { name: 'Orders' }));
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search orders' }), '0002');
    expect(within(screen.getByRole('table')).getAllByText(/SD-AAAA-/)).toHaveLength(1);

    await userEvent.click(screen.getByRole('tab', { name: 'Developer settings' }));
    expect(screen.getByText(/merchant-api\/orders/)).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Merchant API Keys' })).toBeInTheDocument();
  });

  it('works out the summary numbers in Sri Lanka days', () => {
    // 14 Nov 2026 00:30 in Colombo
    const stats = orderStats(orders.concat(order('0005', 'CREATED', '2026-11-13T19:10:00Z')), new Date('2026-11-13T19:00:00Z'));
    expect(stats).toEqual({ today: 1, todayWaiting: 1, live: 3, deliveredWeek: 1, failedWeek: 1 });
  });
});

describe('AdminMerchantsPage', () => {
  it('lists real pending and approved merchants and approves one', async () => {
    let approved = false;
    const fetchMock = stubApi({
      'status=PENDING': () => json(200, { content: approved ? [] : [{ ...MERCHANT, id: 'm-2', businessName: 'New Shop', status: 'PENDING' }], totalElements: 1 }),
      'status=APPROVED': () => json(200, { content: approved ? [MERCHANT, { ...MERCHANT, id: 'm-2', businessName: 'New Shop' }] : [MERCHANT], totalElements: 1 }),
      '/approve': () => { approved = true; return json(200, { ...MERCHANT, id: 'm-2' }); },
    });
    render(<AdminMerchantsPage adminToken="admin" />);

    const card = await screen.findByRole('article', { name: 'New Shop' });
    expect(screen.getByText('Kandy Spice House')).toBeInTheDocument();
    await userEvent.click(within(card).getByRole('button', { name: 'Approve' }));

    expect(await screen.findByRole('status')).toHaveTextContent('New Shop is approved');
    expect(await screen.findByText(/No applications are waiting for review/)).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url, init]) => String(url).includes('/m-2/approve') && (init as RequestInit).method === 'PATCH')).toBe(true);
  });

  it('rejects with a reason typed on the page', async () => {
    const fetchMock = stubApi({
      'status=PENDING': () => json(200, { content: [{ ...MERCHANT, id: 'm-2', businessName: 'New Shop', status: 'PENDING' }] }),
      'status=APPROVED': () => json(200, { content: [] }),
      '/reject': () => json(200, { ...MERCHANT, status: 'REJECTED' }),
    });
    render(<AdminMerchantsPage adminToken="admin" />);

    const card = await screen.findByRole('article', { name: 'New Shop' });
    await userEvent.click(within(card).getByRole('button', { name: 'Reject' }));
    const submit = within(card).getByRole('button', { name: 'Reject application' });
    expect(submit).toBeDisabled();
    await userEvent.type(within(card).getByLabelText(/Reason/), 'BRN could not be verified');
    await userEvent.click(submit);

    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([url]) => String(url).includes('/reject'));
      expect(JSON.parse((call![1] as RequestInit).body as string)).toEqual({ reason: 'BRN could not be verified' });
    });
  });
});

describe('ApiKeysPage', () => {
  it('asks on the page before revoking a key', async () => {
    const fetchMock = stubApi({
      '/api-keys/k-1': () => json(204, null),
      '/api-keys': () => json(200, [{ id: 'k-1', name: 'Online store', keyPrefix: 'sd_live_ab', revoked: false, createdAt: '2026-11-01T00:00:00Z' }]),
    });
    render(<ApiKeysPage token="t" />);

    await userEvent.click(await screen.findByRole('button', { name: 'Revoke' }));
    expect(screen.getByText('Revoke for good?')).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: 'Revoke' })[0]);

    await waitFor(() =>
      expect(fetchMock.mock.calls.some(([url, init]) => String(url).includes('/api-keys/k-1') && (init as RequestInit).method === 'DELETE')).toBe(true),
    );
  });
});
