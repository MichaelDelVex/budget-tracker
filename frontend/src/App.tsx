import { useEffect, useState } from 'react';
import { AppLayout, type RouteKey } from './components/AppLayout';
import { CategoriesTagsPage } from './pages/CategoriesTagsPage';
import { DashboardPage } from './pages/DashboardPage';
import { ImportPage } from './pages/ImportPage';
import { PropertyReportPage } from './pages/PropertyReportPage';
import { RulesPage } from './pages/RulesPage';
import { TransactionsPage } from './pages/TransactionsPage';

const routes = new Set<RouteKey>([
  'dashboard',
  'transactions',
  'import',
  'categories-tags',
  'rules',
  'property-report',
]);

function App() {
  const [route, setRoute] = useState<RouteKey>(readRoute());

  useEffect(() => {
    function onHashChange() {
      setRoute(readRoute());
    }
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  function navigate(nextRoute: RouteKey) {
    window.location.hash = nextRoute;
    setRoute(nextRoute);
  }

  return (
    <AppLayout activeRoute={route} onNavigate={navigate}>
      {route === 'dashboard' ? <DashboardPage /> : null}
      {route === 'transactions' ? <TransactionsPage /> : null}
      {route === 'import' ? <ImportPage /> : null}
      {route === 'categories-tags' ? <CategoriesTagsPage /> : null}
      {route === 'rules' ? <RulesPage /> : null}
      {route === 'property-report' ? <PropertyReportPage /> : null}
    </AppLayout>
  );
}

function readRoute(): RouteKey {
  const route = window.location.hash.replace('#/', '').replace('#', '') as RouteKey;
  return routes.has(route) ? route : 'dashboard';
}

export default App;
