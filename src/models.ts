export interface Transaction {
    id: string;
    date: string;
    account: string;
    merchantId?: string;
    merchant: string;
    description: string;
    amount: number;
    category_id: number;
    transactionType: string;
    status: string;
    currency: string;
}