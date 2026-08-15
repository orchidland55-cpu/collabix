-- HR workflow contract changes
-- 1. Interview position field
-- 2. Remap candidate sources to the six agreed options
-- 3. Remap interview types to EMPLOYEE / INTERN / COMMERCIAL

ALTER TABLE hr_interviews
    ADD COLUMN position VARCHAR(150);

UPDATE hr_candidates
SET source = NULL
WHERE source IS NOT NULL
  AND source NOT IN ('LINKEDIN', 'INDEED', 'JOBLY', 'GMAIL', 'FACEBOOK', 'WEBSITE');

UPDATE hr_interviews
SET type = 'EMPLOYEE'
WHERE type NOT IN ('EMPLOYEE', 'INTERN', 'COMMERCIAL');