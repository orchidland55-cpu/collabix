-- Add interview notes field
ALTER TABLE hr_interviews
    ADD COLUMN notes VARCHAR(10000);
