import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';

const accounts = [{ id: 1, name: 'Everyday', bank: 'Bank', accountType: 'CHECKING', createdAt: '', updatedAt: '' }];
const categories = [
  { id: 2, name: 'Dining', type: 'EXPENSE', defaultCategory: true, active: true, sortOrder: 10, createdAt: '', updatedAt: '' },
  { id: 7, name: 'Travel', type: 'EXPENSE', defaultCategory: false, active: true, sortOrder: 20, createdAt: '', updatedAt: '' },
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
const summaryReport = {
  totalIncome: 1000,
  totalExpenses: 250,
  netSavings: 750,
  savingsPercentage: 75,
  transactionCount: 2,
};
const spendingByCategoryReport = [
  { categoryId: 2, categoryName: 'Dining', totalAmount: 120, percentageOfExpenses: 48 },
];
const incomeVsExpensesReport = [
  { period: '2026-06', totalIncome: 1000, totalExpenses: 250, netSavings: 750 },
];
const propertyReport = {
  rentalIncome: 2000,
  mortgage: 1200,
  insurance: 100,
  rates: 250,
  repairs: 300,
  propertyManagementFees: 150,
  otherPropertyExpenses: 75,
  totalPropertyIncome: 2000,
  totalPropertyExpenses: 2075,
  netPropertyPosition: -75,
};

describe('App', () => {
  beforeEach(() => {
    window.location.hash = '';
    vi.stubGlobal('fetch', vi.fn(mockFetch));
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
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
    expect(screen.getByText('Transaction count')).toBeInTheDocument();
  });

  it('renders dashboard charts', async () => {
    render(<App />);

    expect(await screen.findByLabelText(/spending by category chart/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/income vs expenses chart/i)).toBeInTheDocument();
  });

  it('displays empty report state', async () => {
    vi.stubGlobal('fetch', vi.fn(mockEmptyReportFetch));

    render(<App />);

    expect(await screen.findByText(/no transactions yet/i)).toBeInTheDocument();
    expect(screen.getByText(/no spending data/i)).toBeInTheDocument();
    expect(screen.getByText(/no income or expenses/i)).toBeInTheDocument();
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

  it('paginates transactions so more than the first page is reachable', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn(mockPaginatedTransactionFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /transactions/i }));

    expect(await screen.findByText(/showing 1-50 of 75 transactions/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /^next$/i }));

    await waitFor(() => {
      const calls = fetchCalls().map((url) => url.toString());
      expect(calls.some((url) => url.includes('/api/transactions') && url.includes('page=1'))).toBe(true);
    });
    expect(await screen.findByText(/showing 51-75 of 75 transactions/i)).toBeInTheDocument();
  });

  it('renders account management and creates an account', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^accounts$/i }));
    expect(await screen.findByRole('heading', { name: /^accounts$/i })).toBeInTheDocument();
    await user.type(screen.getByLabelText(/account name/i), 'Holiday');
    await user.type(screen.getByLabelText(/bank name/i), 'NAB');
    await user.selectOptions(screen.getByLabelText(/account type/i), 'SAVINGS');
    await user.click(screen.getByRole('button', { name: /add account/i }));

    await waitFor(() => {
      expect(fetchCalls().some((url) => url.toString() === '/api/accounts')).toBe(true);
      expect(vi.mocked(fetch).mock.calls.some(([, options]) => options?.method === 'POST')).toBe(true);
    });
  });

  it('does not delete account when confirmation is cancelled', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^accounts$/i }));
    await screen.findByText('Everyday');
    await user.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(confirm).toHaveBeenCalled();
    expect(deleteCalls('/api/accounts/1')).toHaveLength(0);
  });

  it('calls safe account delete when confirmation is accepted', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^accounts$/i }));
    await screen.findByText('Everyday');
    await user.click(screen.getByRole('button', { name: /^delete$/i }));

    await waitFor(() => {
      expect(deleteCalls('/api/accounts/1')).toHaveLength(1);
    });
  });

  it('shows account delete errors when account still has transactions', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.stubGlobal('fetch', vi.fn(mockAccountDeleteFailureFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^accounts$/i }));
    await screen.findByText('Everyday');
    await user.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(await screen.findByText(/still referenced/i)).toBeInTheDocument();
    expect(screen.getByText('Everyday')).toBeInTheDocument();
  });

  it('does not nuke account when typed name does not match', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'prompt').mockReturnValue('Wrong name');
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^accounts$/i }));
    await screen.findByText('Everyday');
    await user.click(screen.getByRole('button', { name: /delete with transactions/i }));

    expect(await screen.findByText(/typed account name did not match/i)).toBeInTheDocument();
    expect(deleteCalls('/api/accounts/1/with-transactions')).toHaveLength(0);
  });

  it('nukes account when typed name matches', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'prompt').mockReturnValue('Everyday');
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^accounts$/i }));
    await screen.findByText('Everyday');
    await user.click(screen.getByRole('button', { name: /delete with transactions/i }));

    await waitFor(() => {
      expect(deleteCalls('/api/accounts/1/with-transactions')).toHaveLength(1);
    });
  });

  it('validates missing account and file on import form', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^import$/i }));
    await user.click(screen.getByRole('button', { name: /import transactions/i }));

    expect(await screen.findByText(/choose an account and csv file/i)).toBeInTheDocument();
    expect(screen.getByText(/accountId: choose the account/i)).toBeInTheDocument();
    expect(screen.getByText(/file: choose a csv file/i)).toBeInTheDocument();
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

  it('displays duplicate transaction matches after import', async () => {
    const user = userEvent.setup();
    const file = new File(['Date,Description,Amount'], 'transactions.csv', { type: 'text/csv' });
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^import$/i }));
    await user.selectOptions(await screen.findByLabelText(/account/i), '1');
    await user.upload(screen.getByLabelText(/csv file/i), file);
    await user.click(screen.getByRole('button', { name: /import transactions/i }));

    const duplicates = await screen.findByLabelText(/duplicate transactions/i);
    expect(within(duplicates).getByText(/row 2/i)).toBeInTheDocument();
    expect(within(duplicates).getByText(/transaction #5/i)).toBeInTheDocument();
    expect(within(duplicates).getAllByText(/coffee shop/i)).toHaveLength(2);
  });

  it('displays failed import errors from the API', async () => {
    const user = userEvent.setup();
    const file = new File(['Date,Description,Amount'], 'transactions.csv', { type: 'text/csv' });
    vi.stubGlobal('fetch', vi.fn(mockImportFailureFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^import$/i }));
    await user.selectOptions(await screen.findByLabelText(/account/i), '1');
    await user.upload(screen.getByLabelText(/csv file/i), file);
    await user.click(screen.getByRole('button', { name: /import transactions/i }));

    expect(await screen.findByText(/unable to import csv/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/import summary/i)).not.toBeInTheDocument();
  });

  it('displays oversized CSV upload errors', async () => {
    const user = userEvent.setup();
    const file = new File(['Date,Description,Amount'], 'large-transactions.csv', { type: 'text/csv' });
    vi.stubGlobal('fetch', vi.fn(mockOversizedUploadFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^import$/i }));
    await user.selectOptions(await screen.findByLabelText(/account/i), '1');
    await user.upload(screen.getByLabelText(/csv file/i), file);
    await user.click(screen.getByRole('button', { name: /import transactions/i }));

    expect(await screen.findByText(/csv file is too large/i)).toBeInTheDocument();
  });

  it('shows transaction update errors and restores the previous assignment', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn(mockTransactionUpdateFailureFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /transactions/i }));

    const categorySelect = await screen.findByLabelText(/category for coffee shop/i);
    expect(categorySelect).toHaveValue('2');
    await user.selectOptions(categorySelect, '7');

    expect(await screen.findByText(/transaction could not be updated/i)).toBeInTheDocument();
    expect(categorySelect).toHaveValue('2');
    expect(screen.queryByText(/updated successfully/i)).not.toBeInTheDocument();
  });

  it('shows delete errors and keeps the item visible when deletion fails', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.stubGlobal('fetch', vi.fn(mockDeleteFailureFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));
    expect(await screen.findByText('Dining')).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: /^delete$/i })[0]);

    expect(await screen.findByText(/category is still used by transactions/i)).toBeInTheDocument();
    expect(screen.getByText('Dining')).toBeInTheDocument();
  });

  it('does not call delete API when category delete confirmation is cancelled', async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));
    await screen.findByText('Dining');
    await user.click(screen.getAllByRole('button', { name: /^delete$/i })[0]);

    expect(confirm).toHaveBeenCalledWith('Delete Dining? This cannot be undone.');
    expect(deleteCalls('/api/categories/2')).toHaveLength(0);
  });

  it('calls delete API when category delete confirmation is accepted', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));
    await screen.findByText('Dining');
    await user.click(screen.getAllByRole('button', { name: /^delete$/i })[0]);

    await waitFor(() => {
      expect(deleteCalls('/api/categories/2')).toHaveLength(1);
    });
  });

  it('confirms tag deletion before calling delete API', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));
    await screen.findByText('Tax');
    await user.click(screen.getAllByRole('button', { name: /^delete$/i })[2]);

    await waitFor(() => {
      expect(deleteCalls('/api/tags/3')).toHaveLength(1);
    });
  });

  it('confirms rule deletion before calling delete API', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^rules$/i }));
    await screen.findByText('coffee');
    await user.click(screen.getByRole('button', { name: /^delete$/i }));

    await waitFor(() => {
      expect(deleteCalls('/api/categorisation-rules/4')).toHaveLength(1);
    });
  });

  it('shows a dashboard error state when report requests fail', async () => {
    vi.stubGlobal('fetch', vi.fn(mockReportFailureFetch));
    render(<App />);

    expect(await screen.findByText(/unable to load reports/i)).toBeInTheDocument();
    expect(screen.getByText('Income')).toBeInTheDocument();
  });

  it('renders category and tag forms', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));

    expect(await screen.findByRole('heading', { name: /categories and tags/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/category name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/tag name/i)).toBeInTheDocument();
  });

  it('validates category form input', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /categories & tags/i }));
    await user.click(await screen.findByRole('button', { name: /add category/i }));

    expect(await screen.findByText(/category name is required/i)).toBeInTheDocument();
    expect(screen.getByText(/name: enter a category name/i)).toBeInTheDocument();
  });

  it('renders rule form', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /^rules$/i }));

    expect(await screen.findByRole('heading', { name: /categorisation rules/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/match text/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^category$/i)).toBeInTheDocument();
  });

  it('renders property report data', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(screen.getByRole('button', { name: /property report/i }));

    expect(await screen.findByRole('heading', { name: /property report/i })).toBeInTheDocument();
    expect(screen.getByText('Rental Income')).toBeInTheDocument();
    expect(screen.getByText('Mortgage')).toBeInTheDocument();
    expect(screen.getByText('-$75.00')).toBeInTheDocument();
  });

  it('displays empty property report state', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn(mockEmptyReportFetch));
    render(<App />);

    await user.click(screen.getByRole('button', { name: /property report/i }));

    expect(await screen.findByText(/no property report data/i)).toBeInTheDocument();
  });
});

async function mockFetch(input: RequestInfo | URL, options?: RequestInit) {
  const url = input.toString();
  if (url.startsWith('/api/accounts')) {
    if (options?.method === 'POST' || options?.method === 'PUT') {
      return json({ id: 2, name: 'Holiday', bank: 'NAB', accountType: 'SAVINGS', createdAt: '', updatedAt: '' });
    }
    if (options?.method === 'DELETE') {
      return Promise.resolve(new Response(null, { status: 204 }));
    }
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
  if (url.startsWith('/api/reports/summary')) {
    return json(summaryReport);
  }
  if (url.startsWith('/api/reports/spending-by-category')) {
    return json(spendingByCategoryReport);
  }
  if (url.startsWith('/api/reports/income-vs-expenses')) {
    return json(incomeVsExpensesReport);
  }
  if (url.startsWith('/api/reports/property')) {
    return json(propertyReport);
  }
  if (url.startsWith('/api/transactions')) {
    return json(transactions);
  }
  if (url.startsWith('/api/imports/transactions')) {
    return json({
      totalRows: 2,
      importedCount: 1,
      duplicateCount: 1,
      failedCount: 0,
      errors: [],
      duplicates: [{
        incoming: {
          id: null,
          rowNumber: 2,
          transactionDate: '2026-01-10',
          description: 'Coffee Shop',
          amount: 4.5,
          direction: 'EXPENSE',
        },
        matchedTransaction: {
          id: 5,
          rowNumber: null,
          transactionDate: '2026-01-10',
          description: 'Coffee Shop',
          amount: 4.5,
          direction: 'EXPENSE',
        },
      }],
    });
  }
  return json({});
}

async function mockEmptyReportFetch(input: RequestInfo | URL) {
  const url = input.toString();
  if (url.startsWith('/api/reports/summary')) {
    return json({ totalIncome: 0, totalExpenses: 0, netSavings: 0, savingsPercentage: 0, transactionCount: 0 });
  }
  if (url.startsWith('/api/reports/property')) {
    return json({
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
    });
  }
  if (url.startsWith('/api/reports/spending-by-category') || url.startsWith('/api/reports/income-vs-expenses')) {
    return json([]);
  }
  return mockFetch(input);
}

async function mockPaginatedTransactionFetch(input: RequestInfo | URL) {
  const url = input.toString();
  if (url.startsWith('/api/transactions')) {
    const params = new URLSearchParams(url.split('?')[1] ?? '');
    const page = Number(params.get('page') ?? '0');
    const pageTransactions = page === 0
      ? Array.from({ length: 50 }, (_, index) => transactionItem(index + 1))
      : Array.from({ length: 25 }, (_, index) => transactionItem(index + 51));

    return json({
      content: pageTransactions,
      totalElements: 75,
      totalPages: 2,
      page,
      size: 50,
    });
  }

  return mockFetch(input);
}

async function mockOversizedUploadFetch(input: RequestInfo | URL) {
  const url = input.toString();
  if (url.startsWith('/api/imports/transactions')) {
    return Promise.resolve(new Response(JSON.stringify({
      message: 'CSV file is too large. Use a file up to 5 MB.',
      fields: {},
    }), {
      status: 413,
      headers: { 'Content-Type': 'application/json' },
    }));
  }

  return mockFetch(input);
}

async function mockImportFailureFetch(input: RequestInfo | URL) {
  const url = input.toString();
  if (url.startsWith('/api/imports/transactions')) {
    return Promise.resolve(new Response(JSON.stringify({
      message: 'Unable to import CSV. Check the file and try again.',
      fields: {},
    }), {
      status: 400,
      headers: { 'Content-Type': 'application/json' },
    }));
  }

  return mockFetch(input);
}

async function mockTransactionUpdateFailureFetch(input: RequestInfo | URL, options?: RequestInit) {
  const url = input.toString();
  if (url.startsWith('/api/transactions/5') && options?.method === 'PUT') {
    return Promise.resolve(new Response(JSON.stringify({
      message: 'Transaction could not be updated.',
      fields: {},
    }), {
      status: 409,
      headers: { 'Content-Type': 'application/json' },
    }));
  }

  return mockFetch(input);
}

async function mockDeleteFailureFetch(input: RequestInfo | URL, options?: RequestInit) {
  const url = input.toString();
  if (url.startsWith('/api/categories/2') && options?.method === 'DELETE') {
    return Promise.resolve(new Response(JSON.stringify({
      message: 'Category is still used by transactions.',
      fields: {},
    }), {
      status: 409,
      headers: { 'Content-Type': 'application/json' },
    }));
  }

  return mockFetch(input);
}

async function mockAccountDeleteFailureFetch(input: RequestInfo | URL, options?: RequestInit) {
  const url = input.toString();
  if (url.startsWith('/api/accounts/1') && options?.method === 'DELETE') {
    return Promise.resolve(new Response(JSON.stringify({
      message: 'Cannot delete or update this resource because it is still referenced.',
      fields: {},
    }), {
      status: 409,
      headers: { 'Content-Type': 'application/json' },
    }));
  }

  return mockFetch(input);
}

async function mockReportFailureFetch(input: RequestInfo | URL) {
  const url = input.toString();
  if (url.startsWith('/api/reports/')) {
    return Promise.resolve(new Response(JSON.stringify({
      message: 'Unable to load reports.',
      fields: {},
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' },
    }));
  }

  return mockFetch(input);
}

function transactionItem(id: number) {
  return {
    ...transactions.content[0],
    id,
    description: `Transaction ${id}`,
  };
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

function deleteCalls(path: string) {
  return vi.mocked(fetch).mock.calls.filter(([url, options]) => (
    url.toString().startsWith(path) && options?.method === 'DELETE'
  ));
}
