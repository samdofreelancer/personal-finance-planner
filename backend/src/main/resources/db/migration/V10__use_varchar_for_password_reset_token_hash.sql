-- Match the JPA entity's String mapping (VARCHAR(64)). V9 used CHAR(64),
-- which PostgreSQL reports as bpchar and Hibernate rejects with ddl-auto=validate.
ALTER TABLE password_reset_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64)
    USING rtrim(token_hash);
