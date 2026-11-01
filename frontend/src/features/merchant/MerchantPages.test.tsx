import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MerchantRegisterPage } from './MerchantRegisterPage';
import { PendingApprovalPage } from './PendingApprovalPage';
import { MerchantDashboardPage } from './MerchantDashboardPage';
import { AdminMerchantsPage } from './AdminMerchantsPage';

describe('Merchant Pages (Figma Design Compliance)', () => {
  describe('MerchantRegisterPage (SD/Merchant/01 Registration)', () => {
    it('renders header, 3 registration cards, and terms checkbox', () => {
      render(<MerchantRegisterPage />);

      expect(screen.getByRole('heading', { name: 'Register your business' })).toBeInTheDocument();
      expect(screen.getByText('1. Business details')).toBeInTheDocument();
      expect(screen.getByText('2. Contact person')).toBeInTheDocument();
      expect(screen.getByText('3. Verification documents')).toBeInTheDocument();
      expect(screen.getByText(/I confirm this business is legally registered/)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Submit for approval' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Fill with sample business' })).toBeInTheDocument();
    });

    it('populates sample business data when "Fill with sample business" is clicked', () => {
      render(<MerchantRegisterPage />);

      fireEvent.click(screen.getByRole('button', { name: 'Fill with sample business' }));

      expect(screen.getByLabelText(/Business Name/)).toHaveValue('Lotus Leaf Bakery (Pvt) Ltd');
      expect(screen.getByLabelText(/Business Registration Number/)).toHaveValue('PV-104928');
      expect(screen.getByLabelText(/Full Name/)).toHaveValue('Ayesha Rodrigo');
      expect(screen.getByLabelText(/Work Email/)).toHaveValue('ayesha@lotusleaf.lk');
      expect(screen.getByText('br-certificate.pdf')).toBeInTheDocument();
      expect(screen.getByText('nic-front-back.pdf')).toBeInTheDocument();
    });
  });

  describe('PendingApprovalPage (SD/Merchant/02 Approval pending)', () => {
    it('renders the 3-step verification tracker and business details summary', () => {
      const mockNav = vi.fn();
      render(<PendingApprovalPage onNavigateToDashboard={mockNav} />);

      expect(screen.getByRole('heading', { name: 'Your application is being reviewed' })).toBeInTheDocument();
      expect(screen.getByText('1. Application submitted')).toBeInTheDocument();
      expect(screen.getByText('2. Documents check by SwiftDrop team')).toBeInTheDocument();
      expect(screen.getByText('3. Account activated · start shipping')).toBeInTheDocument();
      expect(screen.getAllByText('Lotus Leaf Bakery (Pvt) Ltd').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('PV-104928')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'View Dashboard Preview' })).toBeInTheDocument();
    });
  });

  describe('MerchantDashboardPage (SD/Merchant/03 Overview & 06 Developer settings)', () => {
    it('renders sidebar, top header, 4 summary metric cards and recent orders table', () => {
      render(<MerchantDashboardPage />);

      // Top bar
      expect(screen.getByRole('banner')).toHaveTextContent('Merchant portal');
      expect(screen.getByPlaceholderText(/Search orders/)).toBeInTheDocument();

      // Heading & 4 metrics
      expect(screen.getByRole('heading', { name: 'Business overview' })).toBeInTheDocument();
      expect(screen.getByText('Orders today')).toBeInTheDocument();
      expect(screen.getByText('Live right now')).toBeInTheDocument();
      expect(screen.getByText('Delivered this week')).toBeInTheDocument();
      expect(screen.getByText('COD collected')).toBeInTheDocument();

      // Table & orders
      expect(screen.getByText('SD-20479')).toBeInTheDocument();
      expect(screen.getByText('Hashini De Silva')).toBeInTheDocument();
    });

    it('switches between Overview and Developer settings tabs', () => {
      render(<MerchantDashboardPage />);

      const devTab = screen.getByRole('button', { name: 'Developer settings' });
      fireEvent.click(devTab);

      expect(screen.getByRole('heading', { name: 'Developer settings' })).toBeInTheDocument();
      expect(screen.getByText('Sandbox mode')).toBeInTheDocument();
      expect(screen.getByText('API keys')).toBeInTheDocument();
      expect(screen.getByText('Webhooks')).toBeInTheDocument();
      expect(screen.getByText('Create an order by API')).toBeInTheDocument();
    });

    it('opens create order modal when "+ Create order" is clicked', () => {
      render(<MerchantDashboardPage />);

      fireEvent.click(screen.getByRole('button', { name: '+ Create order' }));
      expect(screen.getByRole('heading', { name: 'Create a new parcel delivery' })).toBeInTheDocument();
      expect(screen.getByLabelText(/Recipient Name/)).toBeInTheDocument();
    });
  });

  describe('AdminMerchantsPage (SD/Admin/03 Merchants)', () => {
    it('renders approval queue cards and approved merchants table', () => {
      render(<AdminMerchantsPage />);

      expect(screen.getByRole('heading', { name: 'Merchants' })).toBeInTheDocument();
      expect(screen.getByText('Approval queue')).toBeInTheDocument();
      expect(screen.getByText('3 waiting')).toBeInTheDocument();

      expect(screen.getByText('Spice Route Organics')).toBeInTheDocument();
      expect(screen.getByText('Lotus Leaf Bakery (Pvt) Ltd')).toBeInTheDocument();
      expect(screen.getByText('Ceylon Kraft Paper')).toBeInTheDocument();

      expect(screen.getByText('Approved merchants')).toBeInTheDocument();
      expect(screen.getByText('Urban Artisan Coffee')).toBeInTheDocument();
    });

    it('approves a pending merchant when Approve button is clicked', () => {
      render(<AdminMerchantsPage />);

      const approveButtons = screen.getAllByRole('button', { name: 'Approve' });
      expect(approveButtons.length).toBe(3);

      fireEvent.click(approveButtons[0]);

      expect(screen.getByText(/has been approved/)).toBeInTheDocument();
      expect(screen.getByText('2 waiting')).toBeInTheDocument();
    });
  });
});
