import { db } from "./database";

const transactions = db.prepare(`
    SELECT
        t.date,
        t.merchant,
        t.amount,
        c.name AS category
    FROM transactions t
    LEFT JOIN categories c
        ON t.category_id = c.id
    ORDER BY t.date DESC
    LIMIT 20
`).all();

console.table(transactions);