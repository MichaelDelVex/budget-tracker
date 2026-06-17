import { useEffect, useMemo, useState } from 'react';
import { getTransactions } from '../api/budgetApi';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import { SummaryCard } from '../components/SummaryCard';
import type { Transaction } from '../types/api';

const currency = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });

export function DashboardPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getTransactions({}).then((response) => {
      if (active) {
        setTransactions(response.content);
        setError(null);
      }
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

  const summary = useMemo(() => {
    const income = total(transactions, 'INCOME');
    const expenses = total(transactions, 'EXPENSE');
    const savings = income - expenses;
    const savingsRate = income > 0 ? Math.round((savings / income) * 100) : 0;
    const fortnightTransactions = transactions.filter((transaction) =>
      daysBetween(new Date(transaction.transactionDate), new Date()) <= 14
    );

    return {
      income,
      expenses,
      savings,
      savingsRate,
      fortnightIncome: total(fortnightTransactions, 'INCOME'),
      fortnightExpenses: total(fortnightTransactions, 'EXPENSE'),
    };
  }, [transactions]);

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
        <SummaryCard label="Income" value={currency.format(summary.income)} />
        <SummaryCard label="Expenses" value={currency.format(summary.expenses)} />
        <SummaryCard label="Savings" value={currency.format(summary.savings)} />
        <SummaryCard label="Savings percentage" value={`${summary.savingsRate}%`} />
        <SummaryCard
          label="Current fortnight"
          value={currency.format(summary.fortnightIncome - summary.fortnightExpenses)}
          detail={`${currency.format(summary.fortnightIncome)} in, ${currency.format(summary.fortnightExpenses)} out`}
        />
      </div>
      {!loading && transactions.length === 0 ? (
        <EmptyState title="No transactions yet" detail="Import a CSV to start seeing budget totals." />
      ) : null}
    </section>
  );
}

function total(transactions: Transaction[], direction: 'INCOME' | 'EXPENSE') {
  return transactions
    .filter((transaction) => transaction.direction === direction)
    .reduce((sum, transaction) => sum + Number(transaction.amount), 0);
}

function daysBetween(date: Date, now: Date) {
  return Math.abs(now.getTime() - date.getTime()) / 86_400_000;
}
