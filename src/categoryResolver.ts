import { db } from "./database";

interface CategoryRule {
    category_id: number;
}

export function resolveCategory(
    merchant: string
): number {

    const rule = db.prepare(`
        SELECT category_id
        FROM merchant_rules
        WHERE lower(?) LIKE '%' || lower(keyword) || '%'
        ORDER BY priority DESC
        LIMIT 1
    `)
    .get(merchant) as CategoryRule | undefined;


    if (rule) {
        return rule.category_id;
    }

    return 1; // Uncategorised
}