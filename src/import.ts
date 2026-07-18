import fs from "fs";
import { db } from "./database";
import { mapNabTransaction } from "./mapper";

export function importNabJson(
    filename:string
){
    const file =
        fs.readFileSync(
            filename,
            "utf8"
        );

    const json = JSON.parse(file);

    const transactions =
        json.data.transactions.list;

    const statement =
        db.prepare(`
        INSERT OR IGNORE INTO transactions (
            id,
            date,
            account,
            merchant_id,
            merchant,
            description,
            amount,
            category,
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
            @status,
            @currency
        )

    `);

    const insertMany =
        db.transaction((items:any[]) => {
            for(const item of items){
                statement.run(
                    mapNabTransaction(item)
                );
            }
        });
    insertMany(transactions);
}