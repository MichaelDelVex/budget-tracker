export type AccountType = 'CHECKING' | 'SAVINGS' | 'CREDIT_CARD' | 'CASH' | 'OTHER';
export type CategoryType = 'INCOME' | 'EXPENSE';
export type TransactionDirection = 'INCOME' | 'EXPENSE';

export interface Account {
  id: number;
  name: string;
  bank: string;
  accountType: AccountType;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: number;
  name: string;
  type: CategoryType;
  defaultCategory: boolean;
  active: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface Tag {
  id: number;
  name: string;
  color: string;
  createdAt: string;
  updatedAt: string;
}

export interface CategorisationRule {
  id: number;
  matchText: string;
  categoryId: number;
  tagId: number | null;
  active: boolean;
  priority: number;
  createdAt: string;
  updatedAt: string;
}

export interface Transaction {
  id: number;
  accountId: number;
  transactionDate: string;
  description: string;
  rawDescription: string;
  amount: number;
  direction: TransactionDirection;
  categoryId: number | null;
  tagId: number | null;
  importBatchId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface TransactionFilters {
  dateFrom?: string;
  dateTo?: string;
  accountId?: string;
  categoryId?: string;
  tagId?: string;
  direction?: '' | TransactionDirection;
  search?: string;
  uncategorisedOnly?: boolean;
}

export interface ImportRowError {
  rowNumber: number;
  message: string;
}

export interface ImportDuplicateTransaction {
  id: number | null;
  rowNumber: number | null;
  transactionDate: string;
  description: string;
  rawDescription: string;
  amount: number;
  direction: TransactionDirection;
  categoryId: number | null;
  tagId: number | null;
}

export interface ImportDuplicate {
  incoming: ImportDuplicateTransaction;
  matchedTransaction: ImportDuplicateTransaction;
}

export interface UnmatchedImportCategory {
  name: string;
  type: CategoryType;
  rowCount: number;
  rows: Array<{
    rowNumber: number;
    transactionDate: string;
    description: string;
    amount: number;
    direction: TransactionDirection;
  }>;
}

export interface ImportSummary {
  totalRows: number;
  importedCount: number;
  duplicateCount: number;
  failedCount: number;
  errors: ImportRowError[];
  duplicates: ImportDuplicate[];
  unmatchedCategories: UnmatchedImportCategory[];
}

export type ReportGroupBy = 'FORTNIGHT' | 'MONTH' | 'QUARTER' | 'YEAR';

export interface ReportFilters {
  dateFrom?: string;
  dateTo?: string;
  accountId?: string;
  groupBy?: ReportGroupBy;
}

export interface SummaryReport {
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsPercentage: number;
  transactionCount: number;
}

export interface SpendingByCategoryReportItem {
  categoryId: number | null;
  categoryName: string;
  totalAmount: number;
  percentageOfExpenses: number;
}

export interface IncomeVsExpensesReportItem {
  period: string;
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
}

export interface PropertyReport {
  rentalIncome: number;
  mortgage: number;
  insurance: number;
  rates: number;
  repairs: number;
  propertyManagementFees: number;
  otherPropertyExpenses: number;
  totalPropertyIncome: number;
  totalPropertyExpenses: number;
  netPropertyPosition: number;
}

export interface ApiErrorResponse {
  message: string;
  fields?: Record<string, string>;
}
