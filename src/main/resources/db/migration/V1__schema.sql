CREATE TABLE customers (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_number VARCHAR(50)  NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL
);

CREATE TABLE accounts (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id    UUID        NOT NULL REFERENCES customers(id),
    account_number VARCHAR(50) NOT NULL,
    account_type   VARCHAR(50) NOT NULL
);

CREATE TABLE transactions (
    id                      UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    external_transaction_id VARCHAR(255)   NOT NULL,
    source_system           VARCHAR(50)    NOT NULL,
    customer_id             UUID           NOT NULL REFERENCES customers(id),
    account_id              UUID           REFERENCES accounts(id),
    transaction_date        TIMESTAMP      NOT NULL,
    merchant                VARCHAR(255),
    amount                  NUMERIC(19,2)  NOT NULL,
    currency                VARCHAR(10)    NOT NULL DEFAULT 'ZAR',
    category                VARCHAR(50),
    UNIQUE(external_transaction_id, source_system)
);

CREATE INDEX idx_transactions_customer_date ON transactions(customer_id, transaction_date DESC);
CREATE INDEX idx_transactions_category      ON transactions(category);
