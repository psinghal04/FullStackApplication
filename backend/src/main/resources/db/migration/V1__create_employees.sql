CREATE TABLE employees (
    id UUID PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(50) NOT NULL,
    date_of_hire DATE NOT NULL,
    date_of_termination DATE NULL,
    home_address JSONB NOT NULL,
    mailing_address JSONB NOT NULL,
    telephone_number VARCHAR(30) NOT NULL,
    email_address VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_employees_employee_id ON employees (employee_id);
CREATE INDEX idx_employees_last_name_pattern ON employees (last_name varchar_pattern_ops);

INSERT INTO employees (
    id,
    employee_id,
    first_name,
    last_name,
    job_title,
    date_of_birth,
    gender,
    date_of_hire,
    date_of_termination,
    home_address,
    mailing_address,
    telephone_number,
    email_address,
    created_at,
    updated_at
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'EMP-0001',
    'Alex',
    'Johnson',
    'Software Engineer',
    '1993-06-18',
    'Non-Binary',
    '2022-04-01',
    NULL,
    '{"line1":"101 Main St","city":"Austin","state":"TX","postalCode":"78701","country":"US"}'::jsonb,
    '{"line1":"PO Box 100","city":"Austin","state":"TX","postalCode":"78767","country":"US"}'::jsonb,
    '+1-512-555-0199',
    'alex.johnson@example.com',
    NOW(),
    NOW()
);
