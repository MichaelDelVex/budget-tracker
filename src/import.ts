import fs from "fs";
import { db } from "./database";
import { mapNabTransaction } from "./mapper";

export function importNabJson(filename: string) {
    const file = fs.readFileSync(filename, "utf8");
    const json = JSON.parse(file);

    const transactions = json.data.transactions.list;

    console.log(`Found ${transactions.length} NAB transactions`);

    const checkExisting = db.prepare(`
        SELECT id
        FROM transactions
        WHERE id = ?
    `);

    const insert = db.prepare(`
        INSERT INTO transactions (
            id,
            date,
            account,
            merchant_id,
            merchant,
            description,
            amount,
            category,
            transaction_type,
            status,
            currency
        )
        VALUES (
            @id,
            @date,
            @account,
            @merchantId,
            @merchant,
            @description,
            @amount,
            @category,
            @transactionType,
            @status,
            @currency
        )
    `);

    const update = db.prepare(`
        UPDATE transactions
        SET
            date = @date,
            account = @account,
            merchant_id = @merchantId,
            merchant = @merchant,
            description = @description,
            amount = @amount,
            category = @category,
            transaction_type = @transactionType,
            status = @status,
            currency = @currency
        WHERE id = @id
    `);

    let inserted = 0;
    let updated = 0;
    let unchanged = 0;

    const processTransactions = db.transaction((items: any[]) => {
        for (const item of items) {
            const transaction = mapNabTransaction(item);

            const existing = checkExisting.get(transaction.id);

            if (!existing) {
                insert.run(transaction);
                inserted++;
            } else {
                const result = update.run(transaction);

                if (result.changes > 0) {
                    updated++;
                } else {
                    unchanged++;
                }
            }
        }
    });

    processTransactions(transactions);

    console.log(`
Import summary
--------------
Found:     ${transactions.length}
Inserted:  ${inserted}
Updated:   ${updated}
Unchanged: ${unchanged}
`);
}