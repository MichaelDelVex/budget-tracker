CREATE TABLE account_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    bank TEXT NOT NULL,
    account_type TEXT NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'CASH', 'OTHER')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO account_new (id, name, bank, account_type, created_at, updated_at)
SELECT id, name, 'Unknown', type, created_at, created_at
FROM account;

DROP TABLE account;

ALTER TABLE account_new RENAME TO account;
