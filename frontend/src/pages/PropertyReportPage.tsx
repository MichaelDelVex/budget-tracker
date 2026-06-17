import { useEffect, useState } from 'react';
import { getAccounts, getPropertyReport } from '../api/budgetApi';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import { SummaryCard } from '../components/SummaryCard';
import type { Account, PropertyReport, ReportFilters } from '../types/api';

const currency = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });

const emptyReport: PropertyReport = {
  rentalIncome: 0,
  mortgage: 0,
  insurance: 0,
  rates: 0,
  repairs: 0,
  propertyManagementFees: 0,
  otherPropertyExpenses: 0,
  totalPropertyIncome: 0,
  totalPropertyExpenses: 0,
  netPropertyPosition: 0,
};

export function PropertyReportPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [filters, setFilters] = useState<ReportFilters>({});
  const [report, setReport] = useState<PropertyReport>(emptyReport);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getAccounts()
      .then((result) => {
        if (active) {
          setAccounts(result);
        }
      })
      .catch((exception: Error) => {
        if (active) {
          setError(exception.message);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getPropertyReport(filters)
      .then((result) => {
        if (!active) {
          return;
        }
        setReport(result);
        setError(null);
      })
      .catch((exception: Error) => {
        if (active) {
          setError(exception.message);
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [filters]);

  function updateFilter(key: keyof ReportFilters, value: string) {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
  }

  const isEmpty = Number(report.totalPropertyIncome) === 0 && Number(report.totalPropertyExpenses) === 0;

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Reports</p>
          <h2>Property Report</h2>
        </div>
      </header>

      <div className="form-panel" aria-label="Property report filters">
        <label>
          Date from
          <input
            onChange={(event) => updateFilter('dateFrom', event.target.value)}
            type="date"
            value={filters.dateFrom ?? ''}
          />
        </label>
        <label>
          Date to
          <input
            onChange={(event) => updateFilter('dateTo', event.target.value)}
            type="date"
            value={filters.dateTo ?? ''}
          />
        </label>
        <label>
          Account
          <select
            onChange={(event) => updateFilter('accountId', event.target.value)}
            value={filters.accountId ?? ''}
          >
            <option value="">All accounts</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading ? <LoadingState label="Loading property report" /> : null}
      <ErrorMessage message={error} />

      <div className="summary-grid" aria-label="Property totals">
        <SummaryCard label="Property income" value={currency.format(Number(report.totalPropertyIncome))} />
        <SummaryCard label="Property expenses" value={currency.format(Number(report.totalPropertyExpenses))} />
        <SummaryCard label="Net position" value={currency.format(Number(report.netPropertyPosition))} />
      </div>

      {!loading && isEmpty ? (
        <EmptyState title="No property report data" detail="Tagged property transactions will appear here." />
      ) : null}

      <div className="two-column">
        <section className="work-panel" aria-label="Property income">
          <h3>Income</h3>
          <dl className="report-list">
            <ReportLine label="Rental Income" value={report.rentalIncome} />
            <ReportLine label="Total Property Income" value={report.totalPropertyIncome} strong />
          </dl>
        </section>

        <section className="work-panel" aria-label="Property expenses">
          <h3>Expenses</h3>
          <dl className="report-list">
            <ReportLine label="Mortgage" value={report.mortgage} />
            <ReportLine label="Insurance" value={report.insurance} />
            <ReportLine label="Rates" value={report.rates} />
            <ReportLine label="Repairs" value={report.repairs} />
            <ReportLine label="Property Management" value={report.propertyManagementFees} />
            <ReportLine label="Other Property Expense" value={report.otherPropertyExpenses} />
            <ReportLine label="Total Property Expenses" value={report.totalPropertyExpenses} strong />
          </dl>
        </section>
      </div>
    </section>
  );
}

interface ReportLineProps {
  label: string;
  value: number;
  strong?: boolean;
}

function ReportLine({ label, value, strong = false }: ReportLineProps) {
  return (
    <div className={strong ? 'report-line strong' : 'report-line'}>
      <dt>{label}</dt>
      <dd>{currency.format(Number(value))}</dd>
    </div>
  );
}
