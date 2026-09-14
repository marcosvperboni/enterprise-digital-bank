CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
