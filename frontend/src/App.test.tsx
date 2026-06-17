import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';

const accounts = [{ id: 1, name: 'Everyday', bank: 'Bank', accountType: 'CHECKING', createdAt: '', updatedAt: '' }];
const categories = [
  { id: 2, name: 'Dining', type: 'EXPENSE', defaultCategory: true, active: true, sortOrder: 10, createdAt: '', updatedAt: '' },
];
const tags = [{ id: 3, name: 'Tax', color: '#336699', createdAt: '', updatedAt: '' }];
const rules = [{ id: 4, matchText: 'coffee', categoryId: 2, tagId: 3, active: true, priority: 10, createdAt: '', updatedAt: '' }];
const transactions = {
  content: [
    {
      id: 5,
      accountId: 1,
      transactionDate: '2026-06-10',
      description: 'Coffee Shop',
      rawDescription: 'COFFEE SHOP',
      amount: 4.5,
      direction: 'EXPENSE',
      categoryId: 2,
      tagId: 3,
      importBatchId: 1,
      createdAt: '',
      updatedAt: '',
    },
    {
      id: 6,
      accountId: 1,
      transactionDate: '2026-06-11',
      description: 'Salary',
      rawDescription: 'SALARY',
      amount: 1000,
      direction: 'INCOME',
      categoryId: null,
      tagId: null,
      importBatchId: 1,
      createdAt: '',
      updatedAt: '',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  page: 0,
  size: 50,
};

describe('App', () => {
  beforeEach(() => {
    window.location.hash = '';
    vi.stubGlobal('fetch', vi.fn(mockFetch));
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('renders the app shell', async () => {
    render(<App />);

    expect(screen.getByRole('heading', { name: /budget tracker/i })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
  });

  it('renders dashboard summary cards', async () => {
    render(<App />);

    expect(await screen.findByText('Income')).toBeInTheDocument();
    expect(screen.getByText('Expenses')).toBeInTheDocument();
    expect(screen.getByText('Savings')).toBeInTheDocument();
    expect(screen.getByText('Savings percentage')).toBeInTheDocument();
    expect(screen.getByText('Current fortnight')).toBeInTheDocument();
  });

  it('updates transaction request state from filters', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /transactions/i }));
    await screen.findByRole('heading', { name: /^transactions$/i });
    await user.selectOptions(screen.getByLabelText(/direction/i), 'EXPENSE');
    await user.type(screen.getByPlaceholderText(/description/i), 'coffee');

    await waitFor(() => {
      const calls = fetchCalls().map((url) => url.toString());
      expect(calls.some((url) => url.includes('direction=EXPENSE') && url.includes('search=coffee'))).toBe(true);
    });
  });

  it('validates missing account and file on import form', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^import$/i }));
    await user.click(screen.getByRole('button', { name: /import transactions/i }));

    expect(await screen.findByText(/choose an account and csv file/i)).toBeInTheDocument();
  });

  it('displays import summary', async () => {
    const user = userEvent.setup();
    const file = new File(['Date,Description,Amount'], 'transactions.csv', { type: 'text/csv' });
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^import$/i }));
    await user.selectOptions(await screen.findByLabelText(/account/i), '1');
    await user.upload(screen.getByLabelText(/csv file/i), file);
    await user.click(screen.getByRole('button', { name: /import transactions/i }));

    const summary = await screen.findByLabelText(/import summary/i);
    expect(within(summary).getByText(/total rows/i)).toBeInTheDocument();
    expect(within(summary).getByText('2')).toBeInTheDocument();
  });

  it('renders category and tag forms', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));

    expect(await screen.findByRole('heading', { name: /categories and tags/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/category name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/tag name/i)).toBeInTheDocument();
  });

  it('renders rule form', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^rules$/i }));

    expect(await screen.findByRole('heading', { name: /categorisation rules/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/match text/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^category$/i)).toBeInTheDocument();
  });
});

async function mockFetch(input: RequestInfo | URL) {
  const url = input.toString();
  if (url.startsWith('/api/accounts')) {
    return json(accounts);
  }
  if (url.startsWith('/api/categories')) {
    return json(categories);
  }
  if (url.startsWith('/api/tags')) {
    return json(tags);
  }
  if (url.startsWith('/api/categorisation-rules')) {
    return json(rules);
  }
  if (url.startsWith('/api/transactions')) {
    return json(transactions);
  }
  if (url.startsWith('/api/imports/transactions')) {
    return json({ totalRows: 2, importedCount: 1, duplicateCount: 1, failedCount: 0, errors: [] });
  }
  return json({});
}

function json(body: unknown) {
  return Promise.resolve(new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  }));
}

function fetchCalls() {
  return vi.mocked(fetch).mock.calls.map(([url]) => url);
}
