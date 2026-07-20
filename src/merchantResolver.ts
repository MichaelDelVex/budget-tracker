import { db } from "./database";

export function seedMerchantRules() {

    const insert = db.prepare(`
        INSERT OR IGNORE INTO merchant_rules
        (
            keyword,
            category_id
        )
        VALUES
        (
            ?,
            ?
        )
    `);

    const rules = [
        ["woolworths", 3],
        ["coles", 3],
        ["aldi", 3],

        ["uber", 4],

        ["netflix", 8],

        ["luxury escapes", 7]
    ];

    for (const rule of rules) {
        insert.run(
            rule[0],
            rule[1]
        );
    }
}