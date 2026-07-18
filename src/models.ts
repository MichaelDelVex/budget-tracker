export interface Transaction {
    id: string;
    date: string;
    account: string;
    merchantId?: string;
    merchant: string;
    description: string;
    amount: number;
    category: string;
    transactionType: string;
    status: string;
    currency: string;
}