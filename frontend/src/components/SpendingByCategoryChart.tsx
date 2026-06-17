import { EmptyState } from './EmptyState';
import type { SpendingByCategoryReportItem } from '../types/api';

const currency = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });

interface SpendingByCategoryChartProps {
  items: SpendingByCategoryReportItem[];
}

export function SpendingByCategoryChart({ items }: SpendingByCategoryChartProps) {
  if (items.length === 0) {
    return <EmptyState title="No spending data" detail="Expense transactions will appear here once imported." />;
  }

  return (
    <section className="chart-panel" aria-label="Spending by category chart">
      <h3>Spending by category</h3>
      <div className="bar-list">
        {items.map((item) => (
          <div className="bar-row" key={item.categoryId ?? item.categoryName}>
            <span>{item.categoryName}</span>
            <div className="bar-track"><i style={{ width: `${Math.max(item.percentageOfExpenses, 2)}%` }} /></div>
            <strong>{currency.format(Number(item.totalAmount))}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}
