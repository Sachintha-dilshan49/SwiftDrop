import { AuthUser } from "../auth/authApi";
import { DashboardRole } from "../auth/authPortals";

type DashboardItem = {
  title: string;
  detail: string;
  status: string;
};

type DashboardContent = {
  label: string;
  heading: string;
  description: string;
  stats: { label: string; value: string; note: string }[];
  itemsHeading: string;
  items: DashboardItem[];
};

const dashboardContent: Record<DashboardRole, DashboardContent> = {
  CUSTOMER: {
    label: "Customer portal",
    heading: "My deliveries",
    description: "Track your parcels and manage your recent bookings.",
    stats: [
      { label: "Active", value: "2", note: "On the way" },
      { label: "Completed", value: "2", note: "Delivered" },
      { label: "Total deliveries", value: "5", note: "All time" },
    ],
    itemsHeading: "Recent deliveries",
    items: [
      { title: "SD-20481", detail: "Liberty Plaza to Rajagiriya", status: "In transit" },
      { title: "SD-20470", detail: "Duplication Road to Colombo 02", status: "Scheduled" },
      { title: "SD-20455", detail: "Trace Expert City to Nugegoda", status: "Delivered" },
    ],
  },
  MERCHANT: {
    label: "Merchant portal",
    heading: "Business overview",
    description: "Keep an eye on your orders and delivery performance.",
    stats: [
      { label: "Orders today", value: "5", note: "2 awaiting a courier" },
      { label: "Live right now", value: "3", note: "In progress" },
      { label: "Delivered this week", value: "142", note: "98.5% on time" },
      { label: "COD collected", value: "Rs 4,200", note: "Settles Friday" },
    ],
    itemsHeading: "Recent orders",
    items: [
      { title: "SD-20479", detail: "Hashini De Silva · Borella Junction", status: "Awaiting courier" },
      { title: "SD-20478", detail: "Malith Gunasekara · Nugegoda Junction", status: "Awaiting courier" },
      { title: "SD-20476", detail: "Sachini Herath · Liberty Plaza", status: "In transit" },
    ],
  },
  COURIER: {
    label: "Courier portal",
    heading: "Today's deliveries",
    description: "Your assigned stops for today. Only parcels assigned to you are shown.",
    stats: [
      { label: "To deliver", value: "3", note: "Assigned to you" },
      { label: "Delivered", value: "6", note: "Today" },
      { label: "Failed", value: "0", note: "Today" },
    ],
    itemsHeading: "Your delivery list",
    items: [
      { title: "SD-3KQ3-9P2M", detail: "Amaya Perera · No. 42, Galle Road, Colombo 03", status: "Out for delivery" },
      { title: "SD-4M8T-2X9B", detail: "Ishara Fonseka · 18/2 Flower Road, Colombo 07", status: "In transit" },
      { title: "SD-2BN7-5Q8W", detail: "Tharindu Wijesinghe · 55 High Level Road, Maharagama", status: "Created" },
    ],
  },
  DISPATCHER: {
    label: "Dispatch portal",
    heading: "Dispatch overview",
    description: "Monitor today's courier workload and delivery exceptions.",
    stats: [
      { label: "Awaiting assignment", value: "8", note: "Across all zones" },
      { label: "Active couriers", value: "55", note: "On shift today" },
      { label: "At risk", value: "3", note: "Needs attention" },
    ],
    itemsHeading: "Needs attention",
    items: [
      { title: "Delivery exception", detail: "Recipient unavailable · Colombo South", status: "Review" },
      { title: "Courier workload", detail: "One courier is near the delivery limit", status: "Review" },
      { title: "Unassigned orders", detail: "8 new orders need a courier", status: "Assign" },
    ],
  },
  ADMIN: {
    label: "Admin portal",
    heading: "Platform overview",
    description: "SwiftDrop operations across Colombo today.",
    stats: [
      { label: "Gross bookings today", value: "Rs 224,604", note: "+12% vs. last week" },
      { label: "Orders today", value: "367", note: "96.1% delivered on time" },
      { label: "Active couriers", value: "55", note: "8 joined this week" },
      { label: "Merchants to review", value: "3", note: "Applications waiting" },
    ],
    itemsHeading: "Needs attention",
    items: [
      { title: "Merchant applications", detail: "3 businesses are waiting for approval", status: "Review" },
      { title: "Failed deliveries", detail: "1 delivery needs dispatch follow-up", status: "View" },
      { title: "Zone capacity", detail: "Colombo South couriers are near their limit", status: "Review" },
    ],
  },
};

type RoleDashboardProps = {
  role: DashboardRole;
  user: AuthUser;
  onSignOut: () => void;
};

export function RoleDashboard({ role, user, onSignOut }: RoleDashboardProps) {
  const content = dashboardContent[role];

  return (
    <div className={`dashboard dashboard-${role.toLowerCase()}`}>
      <header className="dashboard-header">
        <div className="dashboard-brand">
          <span className="dashboard-brand-mark" aria-hidden="true">S</span>
          <span>SwiftDrop</span>
        </div>
        <span className="dashboard-portal">{content.label}</span>
        <span className="dashboard-page-name">{content.heading}</span>
        <div className="dashboard-user">
          <span className="dashboard-user-name">{user.fullName}</span>
          <button className="dashboard-signout" type="button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="dashboard-heading">
          <div>
            <p className="dashboard-eyebrow">{content.label}</p>
            <h1>Good morning, {user.fullName.split(/\s+/)[0]}</h1>
            <p>{content.description}</p>
          </div>
          <span className="dashboard-data-note">Dashboard preview · sample data</span>
        </div>

        <section className="dashboard-stats" aria-label="Dashboard summary">
          {content.stats.map((stat) => (
            <article className="dashboard-stat" key={stat.label}>
              <span>{stat.label}</span>
              <strong>{stat.value}</strong>
              <small>{stat.note}</small>
            </article>
          ))}
        </section>

        <section className="dashboard-list" aria-labelledby="dashboard-list-heading">
          <div className="dashboard-list-heading">
            <h2 id="dashboard-list-heading">{content.itemsHeading}</h2>
            <span>{content.items.length} shown</span>
          </div>
          <div className="dashboard-items">
            {content.items.map((item) => (
              <article className="dashboard-item" key={item.title}>
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.detail}</p>
                </div>
                <span className="dashboard-status">{item.status}</span>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
