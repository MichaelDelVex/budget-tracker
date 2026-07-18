import Database from "better-sqlite3";

export const db = new Database(
    "./data/budget.db"
);

export function initialiseDatabase(){

    db.exec(`
        CREATE TABLE IF NOT EXISTS transactions (
            id TEXT PRIMARY KEY,
            date TEXT NOT NULL,
            account TEXT,
            merchant_id TEXT,
            merchant TEXT,
            description TEXT,
            amount REAL,
            category TEXT,
            status TEXT,
            currency TEXT,
            transaction_type TEXT,
            imported_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
    `);
}