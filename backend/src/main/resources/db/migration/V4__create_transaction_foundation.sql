CREATE TABLE transaction_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    transaction_date TEXT NOT NULL,
    description TEXT NOT NULL,
    raw_description TEXT NOT NULL,
    amount NUMERIC NOT NULL CHECK (amount > 0),
    direction TEXT NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    category_id INTEGER,
    tag_id INTEGER,
    import_batch_id INTEGER,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_account
        FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_transaction_tag
        FOREIGN KEY (tag_id) REFERENCES tag (id),
    CONSTRAINT fk_transaction_import_batch
        FOREIGN KEY (import_batch_id) REFERENCES import_batch (id)
);

CREATE INDEX idx_transaction_record_transaction_date
    ON transaction_record (transaction_date);

CREATE INDEX idx_transaction_record_account_id
    ON transaction_record (account_id);

CREATE INDEX idx_transaction_record_category_id
    ON transaction_record (category_id);

CREATE INDEX idx_transaction_record_tag_id
    ON transaction_record (tag_id);

CREATE INDEX idx_transaction_record_direction
    ON transaction_record (direction);
