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
    home_address VARCHAR(500) NOT NULL,
    mailing_address VARCHAR(500) NOT NULL,
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
    '8a8e7f8c-b2df-4af6-8b7c-5f74c034f301',
    'HR-ADMIN-0001',
    'Stacey',
    'Smith',
    'HR Administrator',
    DATE '1990-01-15',
    'Female',
    DATE '2021-08-01',
    NULL,
    '500 Market St, Austin, TX 78701, US',
    '500 Market St, Austin, TX 78701, US',
    '+1-512-555-0101',
    'stacey.smith@company.local',
    NOW(),
    NOW()
);
