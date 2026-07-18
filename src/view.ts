import { db } from "./database";

const transactions = db.prepare(`
    SELECT
        date,
        merchant,
        amount,
        category,
        status
    FROM transactions
    ORDER BY date DESC
    LIMIT 10
`).all();

console.table(transactions);