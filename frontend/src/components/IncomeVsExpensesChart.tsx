import { EmptyState } from './EmptyState';
import type { IncomeVsExpensesReportItem } from '../types/api';

interface IncomeVsExpensesChartProps {
  items: IncomeVsExpensesReportItem[];
}

export function IncomeVsExpensesChart({ items }: IncomeVsExpensesChartProps) {
  if (items.length === 0) {
    return <EmptyState title="No income or expenses" detail="Report periods will appear after transactions are imported." />;
  }

  const max = Math.max(...items.flatMap((item) => [Number(item.totalIncome), Number(item.totalExpenses)]), 1);

  return (
    <section className="chart-panel" aria-label="Income vs expenses chart">
      <h3>Income vs expenses</h3>
      <div className="period-chart">
        {items.map((item) => (
          <div className="period-column" key={item.period}>
            <div className="period-bars">
              <i className="income-bar" style={{ height: `${(Number(item.totalIncome) / max) * 100}%` }} />
              <i className="expense-bar" style={{ height: `${(Number(item.totalExpenses) / max) * 100}%` }} />
            </div>
            <span>{item.period}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
