import { useEffect, useState } from 'react';
import { getAccounts, getCategories, getTags, getTransactions, updateTransaction } from '../api/budgetApi';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import type { Account, Category, Tag, Transaction, TransactionFilters } from '../types/api';

const currency = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' });

export function TransactionsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getAccounts(), getCategories(), getTags()])
      .then(([accountsResult, categoriesResult, tagsResult]) => {
        setAccounts(accountsResult);
        setCategories(categoriesResult);
        setTags(tagsResult);
      })
      .catch((exception: Error) => setError(exception.message));
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    getTransactions(filters)
      .then((response) => {
        if (active) {
          setTransactions(response.content);
          setError(null);
        }
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

  function setFilter(key: keyof TransactionFilters, value: string) {
    setFilters((current) => ({ ...current, [key]: value }));
  }

  async function editAssignment(transaction: Transaction, field: 'categoryId' | 'tagId', value: string) {
    const updated = {
      ...transaction,
      [field]: value ? Number(value) : null,
    };
    setTransactions((current) => current.map((item) => item.id === transaction.id ? updated : item));
    try {
      await updateTransaction(updated);
      setError(null);
    } catch (exception) {
      setError((exception as Error).message);
    }
  }

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Review</p>
          <h2>Transactions</h2>
        </div>
      </header>
      <div className="filter-grid" aria-label="Transaction filters">
        <label>Date from<input type="date" onChange={(event) => setFilter('dateFrom', event.target.value)} /></label>
        <label>Date to<input type="date" onChange={(event) => setFilter('dateTo', event.target.value)} /></label>
        <label>Account<select onChange={(event) => setFilter('accountId', event.target.value)}><option value="">All</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></label>
        <label>Category<select onChange={(event) => setFilter('categoryId', event.target.value)}><option value="">All</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
        <label>Tag<select onChange={(event) => setFilter('tagId', event.target.value)}><option value="">All</option>{tags.map((tag) => <option key={tag.id} value={tag.id}>{tag.name}</option>)}</select></label>
        <label>Direction<select onChange={(event) => setFilter('direction', event.target.value)}><option value="">All</option><option value="INCOME">Income</option><option value="EXPENSE">Expense</option></select></label>
        <label className="wide-field">Search<input placeholder="Description" onChange={(event) => setFilter('search', event.target.value)} /></label>
      </div>
      {loading ? <LoadingState label="Loading transactions" /> : null}
      <ErrorMessage message={error} />
      {!loading && transactions.length === 0 ? (
        <EmptyState title="No transactions found" detail="Adjust filters or import a CSV file." />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Direction</th>
                <th>Amount</th>
                <th>Category</th>
                <th>Tag</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((transaction) => (
                <tr key={transaction.id}>
                  <td>{transaction.transactionDate}</td>
                  <td>{transaction.description}</td>
                  <td>{transaction.direction}</td>
                  <td>{currency.format(Number(transaction.amount))}</td>
                  <td><select aria-label={`Category for ${transaction.description}`} value={transaction.categoryId ?? ''} onChange={(event) => editAssignment(transaction, 'categoryId', event.target.value)}><option value="">None</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></td>
                  <td><select aria-label={`Tag for ${transaction.description}`} value={transaction.tagId ?? ''} onChange={(event) => editAssignment(transaction, 'tagId', event.target.value)}><option value="">None</option>{tags.map((tag) => <option key={tag.id} value={tag.id}>{tag.name}</option>)}</select></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
