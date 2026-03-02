ALTER TABLE employees
    ALTER COLUMN home_address TYPE VARCHAR(500)
    USING (
        CASE
            WHEN jsonb_typeof(home_address) = 'string' THEN trim(both '"' from home_address::text)
            ELSE home_address::text
        END
    ),
    ALTER COLUMN mailing_address TYPE VARCHAR(500)
    USING (
        CASE
            WHEN jsonb_typeof(mailing_address) = 'string' THEN trim(both '"' from mailing_address::text)
            ELSE mailing_address::text
        END
    );
