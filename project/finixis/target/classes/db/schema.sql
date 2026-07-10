-- Finixis Database Schema
-- H2 embedded (default) and PostgreSQL compatible
-- balance is computed in the application layer, not via DB triggers

CREATE TABLE IF NOT EXISTS Customer (
    customer_id   INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_name VARCHAR(200)  NOT NULL,
    location      VARCHAR(300),
    contact       VARCHAR(30),
    email         VARCHAR(200),
    creation_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT uq_customer_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS Inventory (
    item_id            INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_name          VARCHAR(200)  NOT NULL,
    available_quantity INTEGER       NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    unit_price         DECIMAL(15,2) NOT NULL CHECK (unit_price >= 0),
    CONSTRAINT uq_inventory_name UNIQUE (item_name)
);

-- Transaction_Credit: money the customer owes us (payment expected or received)
CREATE TABLE IF NOT EXISTS Transaction_Credit (
    transaction_id   INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      INTEGER       NOT NULL,
    total_amount     DECIMAL(15,2) NOT NULL CHECK (total_amount >= 0),
    paid_amount      DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    balance          DECIMAL(15,2) NOT NULL DEFAULT 0,
    transaction_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    is_settled       BOOLEAN       NOT NULL DEFAULT FALSE,
    notes            VARCHAR(500),
    CONSTRAINT fk_tc_customer  FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE RESTRICT
);

-- Line items that make up a single Transaction_Credit
CREATE TABLE IF NOT EXISTS Transaction_Credit_Item (
    line_item_id         INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id       INTEGER       NOT NULL,
    item_id              INTEGER       NOT NULL,
    quantity             INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price_snapshot  DECIMAL(15,2) NOT NULL,
    line_total           DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_tci_transaction FOREIGN KEY (transaction_id) REFERENCES Transaction_Credit(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_tci_item        FOREIGN KEY (item_id) REFERENCES Inventory(item_id) ON DELETE RESTRICT
);

-- Transaction_Debit: money the business owes to the customer
CREATE TABLE IF NOT EXISTS Transaction_Debit (
    debit_id    INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id INTEGER       NOT NULL,
    amount      DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    debit_date  DATE          NOT NULL DEFAULT CURRENT_DATE,
    notes       VARCHAR(500),
    CONSTRAINT fk_td_customer FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE RESTRICT
);

-- Generated_Report: PDF or Excel files produced from the Transaction History page
CREATE TABLE IF NOT EXISTS Generated_Report (
    export_id     INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name     VARCHAR(300)  NOT NULL,
    file_type     VARCHAR(50)   NOT NULL,
    format        VARCHAR(10)   NOT NULL,
    file_path     VARCHAR(1000),
    creation_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common JOIN and filter paths
CREATE INDEX IF NOT EXISTS idx_tc_customer ON Transaction_Credit(customer_id);
CREATE INDEX IF NOT EXISTS idx_tc_date     ON Transaction_Credit(transaction_date);
CREATE INDEX IF NOT EXISTS idx_tc_settled  ON Transaction_Credit(is_settled);
CREATE INDEX IF NOT EXISTS idx_tci_txn     ON Transaction_Credit_Item(transaction_id);
CREATE INDEX IF NOT EXISTS idx_tci_item    ON Transaction_Credit_Item(item_id);
CREATE INDEX IF NOT EXISTS idx_td_customer ON Transaction_Debit(customer_id);
CREATE INDEX IF NOT EXISTS idx_td_date     ON Transaction_Debit(debit_date);
CREATE INDEX IF NOT EXISTS idx_rpt_date    ON Generated_Report(creation_date)
