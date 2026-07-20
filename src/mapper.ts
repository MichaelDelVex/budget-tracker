import { Transaction } from "./models";


export function mapNabTransaction(
    raw: any
): Transaction {
    return {
        id: raw.transactionId,
        date: raw.date,
        account: raw.reference,
        merchantId: raw.merchant?.id,
        merchant: raw.merchant?.title ?? "Unknown",
        description: raw.narrative,
        amount: Number(raw.amount),
        category_id: raw.category?.id ?? "Uncategorised",
        transactionType: raw.transactionType,
        status: raw.processingStatus,
        currency: raw.currency
    };
}