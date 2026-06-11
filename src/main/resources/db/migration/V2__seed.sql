INSERT INTO customers (customer_number, full_name) VALUES
    ('CUST-001', 'Thabo Nkosi'),
    ('CUST-002', 'Naledi Dlamini'),
    ('CUST-003', 'Sipho Mthembu');

INSERT INTO accounts (customer_id, account_number, account_type)
SELECT id, 'ACC-001', 'CHEQUE'  FROM customers WHERE customer_number = 'CUST-001'
UNION ALL
SELECT id, 'ACC-002', 'SAVINGS' FROM customers WHERE customer_number = 'CUST-001'
UNION ALL
SELECT id, 'ACC-003', 'CHEQUE'  FROM customers WHERE customer_number = 'CUST-002'
UNION ALL
SELECT id, 'ACC-004', 'CHEQUE'  FROM customers WHERE customer_number = 'CUST-003';
