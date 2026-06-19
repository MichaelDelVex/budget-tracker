CREATE TABLE csv_category_mapping (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_name TEXT NOT NULL,
    normalized_source_name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_csv_category_mapping_category
        FOREIGN KEY (category_id)
        REFERENCES category(id)
);

CREATE UNIQUE INDEX ux_csv_category_mapping_source_type
    ON csv_category_mapping(normalized_source_name, type);
