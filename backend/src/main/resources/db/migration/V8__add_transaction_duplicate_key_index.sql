CREATE UNIQUE INDEX ux_transactions_duplicate_key
ON transaction_record (account_id, transaction_date, description, amount);
