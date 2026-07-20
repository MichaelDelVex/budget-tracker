import Database from "better-sqlite3";

export const db = new Database(
    "./data/budget.db"
);

export function initialiseDatabase() {
    db.exec(`
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            parent_id INTEGER,
            active INTEGER DEFAULT 1,
            FOREIGN KEY(parent_id)
                REFERENCES categories(id)
        );
        CREATE TABLE IF NOT EXISTS merchant_rules (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            keyword TEXT UNIQUE NOT NULL,
            category_id INTEGER NOT NULL,
            priority INTEGER DEFAULT 0,
            FOREIGN KEY(category_id)
                REFERENCES categories(id)
        );
        CREATE TABLE IF NOT EXISTS transactions (
            id TEXT PRIMARY KEY,
            date TEXT NOT NULL,
            account TEXT,
            merchant_id TEXT,
            merchant TEXT,
            description TEXT,
            amount REAL,
            category_id INTEGER,
            transaction_type TEXT,
            status TEXT,
            currency TEXT,
            imported_at TEXT DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY(category_id)
                REFERENCES categories(id)
        );
    `);
}