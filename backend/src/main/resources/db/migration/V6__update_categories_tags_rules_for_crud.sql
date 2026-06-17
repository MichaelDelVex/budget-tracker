ALTER TABLE category ADD COLUMN updated_at TEXT NOT NULL DEFAULT '1970-01-01 00:00:00';

ALTER TABLE tag ADD COLUMN updated_at TEXT NOT NULL DEFAULT '1970-01-01 00:00:00';

CREATE TABLE categorisation_rule_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_text TEXT NOT NULL,
    category_id INTEGER NOT NULL,
    tag_id INTEGER,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    priority INTEGER NOT NULL DEFAULT 100 CHECK (priority >= 0),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categorisation_rule_category
        FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_categorisation_rule_tag
        FOREIGN KEY (tag_id) REFERENCES tag (id)
);

INSERT INTO categorisation_rule_new (id, match_text, category_id, active, priority, created_at, updated_at)
SELECT id, match_text, category_id, enabled, priority, created_at, CURRENT_TIMESTAMP
FROM categorisation_rule;

DROP TABLE categorisation_rule;

ALTER TABLE categorisation_rule_new RENAME TO categorisation_rule;
