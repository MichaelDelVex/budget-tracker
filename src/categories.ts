import { db } from "./database";

export function seedCategories() {
    const insert = db.prepare(`
        INSERT OR IGNORE INTO categories
        (
            name,
            parent_id
        )
        VALUES
        (
            ?,
            ?
        )
    `);

    const categories = [

        ["Uncategorised", null],

        ["Living", null],
        ["Groceries", 2],
        ["Transport", 2],
        ["Insurance", 2],
        ["Health", 2],

        ["Lifestyle", null],
        ["Eating Out", 7],
        ["Travel", 7],
        ["Entertainment", 7],
        ["Subscriptions", 7],

        ["Financial", null],
        ["Savings", 12],
        ["Investments", 12]

    ];

    for (const category of categories) {
        insert.run(
            category[0],
            category[1]
        );
    }
}