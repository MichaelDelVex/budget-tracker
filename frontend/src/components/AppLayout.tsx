import type { ReactNode } from 'react';

export type RouteKey = 'dashboard' | 'transactions' | 'import' | 'categories-tags' | 'rules' | 'property-report';

const navItems: Array<{ route: RouteKey; label: string }> = [
  { route: 'dashboard', label: 'Dashboard' },
  { route: 'transactions', label: 'Transactions' },
  { route: 'import', label: 'Import' },
  { route: 'categories-tags', label: 'Categories & Tags' },
  { route: 'rules', label: 'Rules' },
  { route: 'property-report', label: 'Property Report' },
];

interface AppLayoutProps {
  activeRoute: RouteKey;
  onNavigate: (route: RouteKey) => void;
  children: ReactNode;
}

export function AppLayout({ activeRoute, onNavigate, children }: AppLayoutProps) {
  return (
    <div className="app-frame">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Local budget</p>
          <h1>Budget Tracker</h1>
        </div>
        <nav aria-label="Primary">
          {navItems.map((item) => (
            <button
              className={item.route === activeRoute ? 'nav-link active' : 'nav-link'}
              key={item.route}
              onClick={() => onNavigate(item.route)}
              type="button"
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
