ALTER TABLE assumptions
    DROP COLUMN loan_principal,
    DROP COLUMN fixed_payment,
    DROP COLUMN nov_interest,
    DROP COLUMN dec_interest,
    DROP COLUMN payoff_interest,
    DROP COLUMN payoff_interest_confirmed;
