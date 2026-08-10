-- =========================================
-- Collabix
-- Version 20260849
-- Add team manager (User) support to teams
-- =========================================

ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS manager_id UUID;

ALTER TABLE teams
    ADD CONSTRAINT fk_teams_manager
        FOREIGN KEY (manager_id)
            REFERENCES users(id)
            ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_teams_manager_id ON teams(manager_id);