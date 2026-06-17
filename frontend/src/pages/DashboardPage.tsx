import { useEffect, useState } from 'react';
import { getIncomeVsExpensesReport, getSpendingByCategoryReport, getSummaryReport } from '../api/budgetApi';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { IncomeVsExpensesChart } from '../components/IncomeVsExpensesChart';
import { LoadingState } from '../components/LoadingState';
import { SpendingByCategoryChart } from '../components/SpendingByCategoryChart';
import { SummaryCard } from '../components/SummaryCard';
import type { IncomeVsExpensesReportItem, SpendingByCategoryReportItem, SummaryReport } from '../types/api';

const currency = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });
const emptySummary: SummaryReport = {
  totalIncome: 0,
  totalExpenses: 0,
  netSavings: 0,
  savingsPercentage: 0,
  transactionCount: 0,
};

export function DashboardPage() {
  const [summary, setSummary] = useState<SummaryReport>(emptySummary);
  const [spendingByCategory, setSpendingByCategory] = useState<SpendingByCategoryReportItem[]>([]);
  const [incomeVsExpenses, setIncomeVsExpenses] = useState<IncomeVsExpensesReportItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.all([
      getSummaryReport(),
      getSpendingByCategoryReport(),
      getIncomeVsExpensesReport({ groupBy: 'MONTH' }),
    ]).then(([summaryResult, spendingResult, incomeVsExpensesResult]) => {
      if (!active) {
        return;
      }
      setSummary(summaryResult);
      setSpendingByCategory(spendingResult);
      setIncomeVsExpenses(incomeVsExpensesResult);
      setError(null);
    }).catch((exception: Error) => {
      if (active) {
        setError(exception.message);
      }
    }).finally(() => {
      if (active) {
        setLoading(false);
      }
    });

    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Overview</p>
          <h2>Dashboard</h2>
        </div>
      </header>
      {loading ? <LoadingState label="Loading dashboard" /> : null}
      <ErrorMessage message={error} />
      <div className="summary-grid" aria-label="Budget summary cards">
        <SummaryCard label="Income" value={currency.format(Number(summary.totalIncome))} />
        <SummaryCard label="Expenses" value={currency.format(Number(summary.totalExpenses))} />
        <SummaryCard label="Savings" value={currency.format(Number(summary.netSavings))} />
        <SummaryCard label="Savings percentage" value={`${Number(summary.savingsPercentage).toFixed(2)}%`} />
        <SummaryCard
          label="Transaction count"
          value={String(summary.transactionCount)}
          detail="Filtered by selected report range"
        />
      </div>
      {!loading && summary.transactionCount === 0 ? (
        <EmptyState title="No transactions yet" detail="Import a CSV to start seeing budget totals." />
      ) : null}
      <div className="two-column">
        <SpendingByCategoryChart items={spendingByCategory} />
        <IncomeVsExpensesChart items={incomeVsExpenses} />
      </div>
    </section>
  );
}
