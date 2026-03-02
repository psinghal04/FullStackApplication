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
)
VALUES (
    '8a8e7f8c-b2df-4af6-8b7c-5f74c034f301',
    'HR-ADMIN-0001',
    'Stacey',
    'Smith',
    'HR Administrator',
    DATE '1990-01-15',
    'Female',
    DATE '2021-08-01',
    NULL,
    '{"line1":"500 Market St","city":"Austin","state":"TX","postalCode":"78701","country":"US"}'::jsonb,
    '{"line1":"500 Market St","city":"Austin","state":"TX","postalCode":"78701","country":"US"}'::jsonb,
    '+1-512-555-0101',
    'stacey.smith@company.local',
    NOW(),
    NOW()
)
ON CONFLICT (employee_id) DO NOTHING;
