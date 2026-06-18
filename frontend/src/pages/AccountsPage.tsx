import { FormEvent, useEffect, useState } from 'react';
import {
  createAccount,
  deleteAccount,
  deleteAccountWithTransactions,
  getAccounts,
  updateAccount,
} from '../api/budgetApi';
import { ApiError } from '../api/client';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import type { Account, AccountType } from '../types/api';

const accountTypes: AccountType[] = ['CHECKING', 'SAVINGS', 'CREDIT_CARD', 'CASH', 'OTHER'];
const emptyAccount = { name: '', bank: '', accountType: 'CHECKING' as AccountType };

export function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [form, setForm] = useState(emptyAccount);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    refresh();
  }, []);

  function refresh() {
    setLoading(true);
    getAccounts()
      .then((result) => {
        setAccounts(result);
        setError(null);
      })
      .catch((exception: Error) => setError(exception.message))
      .finally(() => setLoading(false));
  }

  async function submitAccount(event: FormEvent) {
    event.preventDefault();
    const validationErrors: Record<string, string> = {};
    if (!form.name.trim()) {
      validationErrors.name = 'Enter an account name.';
    }
    if (!form.bank.trim()) {
      validationErrors.bank = 'Enter the bank name.';
    }
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setError('Account details need attention.');
      return;
    }

    try {
      if (editingId) {
        await updateAccount(editingId, form);
      } else {
        await createAccount(form);
      }
      setForm(emptyAccount);
      setEditingId(null);
      setFieldErrors({});
      refresh();
    } catch (exception) {
      setError((exception as Error).message);
      setFieldErrors(exception instanceof ApiError ? exception.fields : {});
    }
  }

  function editAccount(account: Account) {
    setEditingId(account.id);
    setForm({
      name: account.name,
      bank: account.bank,
      accountType: account.accountType,
    });
  }

  function deleteAccountSafely(account: Account) {
    if (!window.confirm(`Delete ${account.name}? This only works when the account has no transactions.`)) {
      return;
    }

    deleteAccount(account.id).then(refresh).catch((exception: Error) => setError(exception.message));
  }

  function nukeAccount(account: Account) {
    const typedName = window.prompt(
      `This will permanently delete ${account.name}, its transactions, and its import history. Type the account name to continue.`
    );
    if (typedName !== account.name) {
      setError('Account was not deleted. The typed account name did not match.');
      return;
    }

    deleteAccountWithTransactions(account.id)
      .then(refresh)
      .catch((exception: Error) => setError(exception.message));
  }

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">Setup</p>
          <h2>Accounts</h2>
        </div>
      </header>
      <ErrorMessage message={error} fields={fieldErrors} />
      {loading ? <LoadingState label="Loading accounts" /> : null}
      <section className="work-panel">
        <h3>{editingId ? 'Edit account' : 'Add account'}</h3>
        <form className="compact-form" onSubmit={submitAccount}>
          <input aria-label="Account name" placeholder="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <input aria-label="Bank name" placeholder="Bank" value={form.bank} onChange={(event) => setForm({ ...form, bank: event.target.value })} />
          <select aria-label="Account type" value={form.accountType} onChange={(event) => setForm({ ...form, accountType: event.target.value as AccountType })}>
            {accountTypes.map((accountType) => <option key={accountType} value={accountType}>{accountType}</option>)}
          </select>
          <button type="submit">{editingId ? 'Save account' : 'Add account'}</button>
        </form>
      </section>
      {!loading && accounts.length === 0 ? <EmptyState title="No accounts" detail="Add an account before importing transactions." /> : null}
      <div className="item-list">
        {accounts.map((account) => (
          <article key={account.id}>
            <span>
              <strong>{account.name}</strong>
              <small>{account.bank} - {account.accountType}</small>
            </span>
            <div>
              <button type="button" onClick={() => editAccount(account)}>Edit</button>
              <button type="button" onClick={() => deleteAccountSafely(account)}>Delete</button>
              <button className="danger-button" type="button" onClick={() => nukeAccount(account)}>
                Delete with transactions
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
