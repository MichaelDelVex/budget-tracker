import { request } from './client';
import type {
  Account,
  AccountType,
  CategorisationRule,
  Category,
  CategoryType,
  ImportSummary,
  IncomeVsExpensesReportItem,
  PagedResponse,
  PropertyReport,
  ReportFilters,
  Tag,
  SpendingByCategoryReportItem,
  SummaryReport,
  Transaction,
  TransactionFilters,
} from '../types/api';

export interface AccountPayload {
  name: string;
  bank: string;
  accountType: AccountType;
}

export interface CategoryPayload {
  name: string;
  type: CategoryType;
  defaultCategory: boolean;
  active: boolean;
  sortOrder: number;
}

export interface TagPayload {
  name: string;
  color: string;
}

export interface RulePayload {
  matchText: string;
  categoryId: number;
  tagId: number | null;
  active: boolean;
  priority: number;
}

export function getAccounts() {
  return request<Account[]>('/api/accounts');
}

export function createAccount(payload: AccountPayload) {
  return request<Account>('/api/accounts', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateAccount(id: number, payload: AccountPayload) {
  return request<Account>(`/api/accounts/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteAccount(id: number) {
  return request<void>(`/api/accounts/${id}`, { method: 'DELETE' });
}

export function deleteAccountWithTransactions(id: number) {
  return request<void>(`/api/accounts/${id}/with-transactions`, { method: 'DELETE' });
}

export function getCategories() {
  return request<Category[]>('/api/categories');
}

export function createCategory(payload: CategoryPayload) {
  return request<Category>('/api/categories', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateCategory(id: number, payload: CategoryPayload) {
  return request<Category>(`/api/categories/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteCategory(id: number) {
  return request<void>(`/api/categories/${id}`, { method: 'DELETE' });
}

export function getTags() {
  return request<Tag[]>('/api/tags');
}

export function createTag(payload: TagPayload) {
  return request<Tag>('/api/tags', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateTag(id: number, payload: TagPayload) {
  return request<Tag>(`/api/tags/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteTag(id: number) {
  return request<void>(`/api/tags/${id}`, { method: 'DELETE' });
}

export function getRules() {
  return request<CategorisationRule[]>('/api/categorisation-rules');
}

export function createRule(payload: RulePayload) {
  return request<CategorisationRule>('/api/categorisation-rules', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateRule(id: number, payload: RulePayload) {
  return request<CategorisationRule>(`/api/categorisation-rules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteRule(id: number) {
  return request<void>(`/api/categorisation-rules/${id}`, { method: 'DELETE' });
}

export function getTransactions(filters: TransactionFilters = {}, page = 0, size = 50) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: 'transactionDate,desc',
  });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, String(value));
    }
  });
  return request<PagedResponse<Transaction>>(`/api/transactions?${params.toString()}`);
}

export function updateTransaction(transaction: Transaction) {
  return request<Transaction>(`/api/transactions/${transaction.id}`, {
    method: 'PUT',
    body: JSON.stringify({
      accountId: transaction.accountId,
      transactionDate: transaction.transactionDate,
      description: transaction.description,
      rawDescription: transaction.rawDescription,
      amount: transaction.amount,
      direction: transaction.direction,
      categoryId: transaction.categoryId,
      tagId: transaction.tagId,
      importBatchId: transaction.importBatchId,
    }),
  });
}

export interface TransactionCreatePayload {
  accountId: number;
  transactionDate: string;
  description: string;
  rawDescription: string;
  amount: number;
  direction: Transaction['direction'];
  categoryId: number | null;
  tagId: number | null;
}

export function createTransaction(payload: TransactionCreatePayload) {
  return request<Transaction>('/api/transactions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function importTransactions(accountId: string, file: File) {
  const data = new FormData();
  data.append('accountId', accountId);
  data.append('file', file);
  return request<ImportSummary>('/api/imports/transactions', { method: 'POST', body: data });
}

export function getSummaryReport(filters: ReportFilters = {}) {
  return request<SummaryReport>(`/api/reports/summary?${reportParams(filters)}`);
}

export function getSpendingByCategoryReport(filters: ReportFilters = {}) {
  return request<SpendingByCategoryReportItem[]>(`/api/reports/spending-by-category?${reportParams(filters)}`);
}

export function getIncomeVsExpensesReport(filters: ReportFilters = {}) {
  return request<IncomeVsExpensesReportItem[]>(`/api/reports/income-vs-expenses?${reportParams(filters)}`);
}

export function getPropertyReport(filters: ReportFilters = {}) {
  return request<PropertyReport>(`/api/reports/property?${reportParams(filters)}`);
}

function reportParams(filters: ReportFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });
  return params.toString();
}
