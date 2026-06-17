CREATE TABLE import_batch_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    original_filename TEXT NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0 CHECK (total_rows >= 0),
    imported_count INTEGER NOT NULL DEFAULT 0 CHECK (imported_count >= 0),
    duplicate_count INTEGER NOT NULL DEFAULT 0 CHECK (duplicate_count >= 0),
    failed_count INTEGER NOT NULL DEFAULT 0 CHECK (failed_count >= 0),
    imported_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_batch_account
        FOREIGN KEY (account_id) REFERENCES account (id)
);

DROP TABLE import_batch;

ALTER TABLE import_batch_new RENAME TO import_batch;
