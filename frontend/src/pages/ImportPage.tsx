import { FormEvent, useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { createCategory, createTransaction, getAccounts, importTransactions } from '../api/budgetApi';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingState } from '../components/LoadingState';
import type { Account, ImportDuplicateTransaction, ImportSummary, UnmatchedImportCategory } from '../types/api';

export function ImportPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountId, setAccountId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [summary, setSummary] = useState<ImportSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const duplicates = summary?.duplicates ?? [];
  const unmatchedCategories = summary?.unmatchedCategories ?? [];

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

  async function addCategory(category: UnmatchedImportCategory) {
    try {
      await createCategory({
        name: category.name,
        type: category.type,
        defaultCategory: false,
        active: true,
        sortOrder: 500,
      });
      setSummary((current) => current ? {
        ...current,
        unmatchedCategories: current.unmatchedCategories.filter((item) => item.name !== category.name || item.type !== category.type),
      } : current);
      setError(null);
    } catch (exception) {
      setError((exception as Error).message);
    }
  }

  async function addDuplicateAnyway(duplicate: ImportSummary['duplicates'][number]) {
    if (!accountId) {
      setError('Choose an account before adding a duplicate transaction.');
      return;
    }

    const suggestedDescription = `${duplicate.incoming.description} #2`;
    const description = window.prompt('Use a distinct description for this separate transaction.', suggestedDescription);
    if (description === null) {
      return;
    }
    if (!description.trim()) {
      setError('Enter a description before adding the duplicate transaction.');
      return;
    }

    try {
      await createTransaction({
        accountId: Number(accountId),
        transactionDate: duplicate.incoming.transactionDate,
        description: description.trim(),
        rawDescription: duplicate.incoming.rawDescription,
        amount: duplicate.incoming.amount,
        direction: duplicate.incoming.direction,
        categoryId: duplicate.matchedTransaction.categoryId,
        tagId: duplicate.matchedTransaction.tagId,
      });
      setSummary((current) => current ? {
        ...current,
        importedCount: current.importedCount + 1,
        duplicateCount: Math.max(0, current.duplicateCount - 1),
        duplicates: current.duplicates.filter((item) => item !== duplicate),
      } : current);
      setError(null);
    } catch (exception) {
      setError((exception as Error).message);
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
          {unmatchedCategories.length > 0 ? (
            <section className="duplicate-review" aria-label="Unmatched CSV categories">
              <h4>New CSV categories found</h4>
              <p className="status-text">Add these as app categories if you want future imports to assign them automatically.</p>
              <ul className="review-list">
                {unmatchedCategories.map((category) => (
                  <li key={`${category.type}-${category.name}`}>
                    <span>{category.name} ({category.type.toLowerCase()}, {category.rowCount} rows)</span>
                    <button type="button" onClick={() => addCategory(category)}>Add category</button>
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
          {duplicates.length > 0 ? (
            <section className="duplicate-review" aria-label="Duplicate transactions">
              <h4>Duplicate transactions</h4>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Uploaded row</th>
                      <th>Uploaded transaction</th>
                      <th>Matched against</th>
                      <th>Matched transaction</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {duplicates.map((duplicate) => (
                      <tr key={`${duplicate.incoming.rowNumber}-${duplicate.incoming.transactionDate}-${duplicate.incoming.description}`}>
                        <td>Row {duplicate.incoming.rowNumber}</td>
                        <td>{formatDuplicateTransaction(duplicate.incoming)}</td>
                        <td>{formatMatchSource(duplicate.matchedTransaction)}</td>
                        <td>{formatDuplicateTransaction(duplicate.matchedTransaction)}</td>
                        <td><button type="button" onClick={() => addDuplicateAnyway(duplicate)}>Add anyway</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

function formatMatchSource(transaction: ImportDuplicateTransaction) {
  if (transaction.id !== null) {
    return `Transaction #${transaction.id}`;
  }

  return transaction.rowNumber ? `Earlier upload row ${transaction.rowNumber}` : 'Existing transaction';
}

function formatDuplicateTransaction(transaction: ImportDuplicateTransaction) {
  return `${transaction.transactionDate} - ${transaction.description} - ${transaction.direction} $${transaction.amount.toFixed(2)}`;
}
