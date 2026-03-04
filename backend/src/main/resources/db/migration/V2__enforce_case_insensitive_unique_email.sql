CREATE UNIQUE INDEX uk_employees_email_address_lower
    ON employees ((lower(email_address)));
