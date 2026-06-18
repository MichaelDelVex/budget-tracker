CREATE TABLE budget_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    active INTEGER NOT NULL DEFAULT 0 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE budget_node (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    budget_profile_id INTEGER NOT NULL,
    parent_node_id INTEGER,
    name TEXT NOT NULL,
    percentage NUMERIC NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    category_id INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_node_profile
        FOREIGN KEY (budget_profile_id) REFERENCES budget_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_node_parent
        FOREIGN KEY (parent_node_id) REFERENCES budget_node (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_node_category
        FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE INDEX idx_budget_node_profile
    ON budget_node (budget_profile_id);

CREATE INDEX idx_budget_node_parent
    ON budget_node (parent_node_id);

CREATE INDEX idx_budget_node_category
    ON budget_node (category_id);

CREATE INDEX idx_budget_node_sort
    ON budget_node (budget_profile_id, parent_node_id, sort_order, name);
