ALTER TABLE assumptions ADD COLUMN forecast_years INTEGER NOT NULL DEFAULT 4;
ALTER TABLE assumptions ADD CONSTRAINT ck_assumptions_forecast_years CHECK (forecast_years BETWEEN 1 AND 6);

CREATE TABLE salary_raise_rates (
    assumption_id BIGINT NOT NULL REFERENCES assumptions(id) ON DELETE CASCADE,
    raise_year INTEGER NOT NULL CHECK (raise_year BETWEEN 2027 AND 2032),
    raise_rate NUMERIC(5, 4) NOT NULL CHECK (raise_rate BETWEEN 0 AND 1),
    PRIMARY KEY (assumption_id, raise_year)
);

INSERT INTO salary_raise_rates (assumption_id, raise_year, raise_rate)
SELECT id, 2027, raise_2027 FROM assumptions
UNION ALL SELECT id, 2028, raise_2028 FROM assumptions
UNION ALL SELECT id, 2029, raise_2029 FROM assumptions
UNION ALL SELECT id, 2030, raise_2030 FROM assumptions;

INSERT INTO salary_raise_rates (assumption_id, raise_year, raise_rate)
SELECT id, years.raise_year, 0 FROM assumptions
CROSS JOIN (VALUES (2031), (2032)) AS years(raise_year);

ALTER TABLE assumptions DROP COLUMN raise_2027;
ALTER TABLE assumptions DROP COLUMN raise_2028;
ALTER TABLE assumptions DROP COLUMN raise_2029;
ALTER TABLE assumptions DROP COLUMN raise_2030;
