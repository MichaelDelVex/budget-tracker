import { db } from "./database";

const rules = db.prepare(`
    SELECT
        mr.keyword,
        c.name AS category
    FROM merchant_rules mr
    JOIN categories c
        ON mr.category_id = c.id
`).all();

console.table(rules);