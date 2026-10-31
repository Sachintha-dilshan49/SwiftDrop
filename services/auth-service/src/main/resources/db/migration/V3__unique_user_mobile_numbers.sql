CREATE UNIQUE INDEX uq_users_mobile_number
    ON users (mobile_number)
    WHERE mobile_number IS NOT NULL AND btrim(mobile_number) <> '';
