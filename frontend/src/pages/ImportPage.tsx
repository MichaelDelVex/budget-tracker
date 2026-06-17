import { FormEvent, useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { getAccounts, importTransactions } from '../api/budgetApi';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import type { Account, ImportSummary } from '../types/api';

export function ImportPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountId, setAccountId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    getAccounts().then(setAccounts).catch((exception: Error) => setError(exception.message));
  }, []);

  async function submitImport(event: FormEvent) {
    event.preventDefault();
    const selectedFile = file;
    const validationErrors: Record<string, string> = {};
    if (!accountId) {
      validationErrors.accountId = 'Choose the account these transactions belong to.';
    }
    if (!selectedFile) {
      validationErrors.file = 'Choose a CSV file to import.';
    } else if (!selectedFile.name.toLowerCase().endsWith('.csv')) {
      validationErrors.file = 'Use a CSV export file.';
    }

    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setError('Choose an account and CSV file before importing.');
      return;
    }
    if (!selectedFile) {
      return;
    }

    setLoading(true);
    try {
      setSummary(await importTransactions(accountId, selectedFile));
      setError(null);
      setFieldErrors({});
    } catch (exception) {
      setError((exception as Error).message);
      setFieldErrors(exception instanceof ApiError ? exception.fields : {});
      setSummary(null);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-stack">
      <header className="page-header">
        <div>
          <p className="eyebrow">CSV upload</p>
          <h2>Import</h2>
        </div>
      </header>
      <form className="form-panel" onSubmit={submitImport}>
        <label>Account<select value={accountId} onChange={(event) => setAccountId(event.target.value)}><option value="">Select account</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></label>
        <label>CSV file<input accept=".csv,text/csv" type="file" onChange={(event) => setFile(event.target.files?.[0] ?? null)} /></label>
        <button type="submit">Import transactions</button>
      </form>
      {loading ? <LoadingState label="Importing CSV" /> : null}
      <ErrorMessage message={error} fields={fieldErrors} />
      {summary ? (
        <section className="result-panel" aria-label="Import summary">
          <h3>Import summary</h3>
          <div className="metric-row">
            <span>Total rows <strong>{summary.totalRows}</strong></span>
            <span>Imported <strong>{summary.importedCount}</strong></span>
            <span>Duplicates <strong>{summary.duplicateCount}</strong></span>
            <span>Failed <strong>{summary.failedCount}</strong></span>
          </div>
          {summary.errors.length > 0 ? (
            <ul className="error-list">
              {summary.errors.map((item) => <li key={`${item.rowNumber}-${item.message}`}>Row {item.rowNumber}: {item.message}</li>)}
            </ul>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}
