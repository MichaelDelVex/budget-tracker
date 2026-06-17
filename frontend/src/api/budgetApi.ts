import { request } from './client';
import type {
  Account,
  CategorisationRule,
  Category,
  CategoryType,
  ImportSummary,
  PagedResponse,
  Tag,
  Transaction,
  TransactionFilters,
} from '../types/api';

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

export function getTransactions(filters: TransactionFilters = {}) {
  const params = new URLSearchParams({ page: '0', size: '50', sort: 'transactionDate,desc' });
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
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

export function importTransactions(accountId: string, file: File) {
  const data = new FormData();
  data.append('accountId', accountId);
  data.append('file', file);
  return request<ImportSummary>('/api/imports/transactions', { method: 'POST', body: data });
}
