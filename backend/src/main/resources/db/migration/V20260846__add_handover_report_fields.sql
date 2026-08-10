-- =============================================================================
-- Add daily-report fields to the Handover Entry / Journal model
-- =============================================================================
-- A HandoverEntry can now be a daily work report (shift, entry date, tasks
-- completed, current progress, pending tasks, blockers, notes, estimate, mood)
-- that is SUBMITTED and later aggregated into an AI-generated HandoverJournal.
-- Classic sender->receiver fields (title, content, receiver) become optional.

-- -----------------------------------------------------------------------------
-- 1) handover_entries - relax workflow columns
-- -----------------------------------------------------------------------------

ALTER TABLE handover_entries ALTER COLUMN title DROP NOT NULL;
ALTER TABLE handover_entries ALTER COLUMN content DROP NOT NULL;

-- -----------------------------------------------------------------------------
-- 2) handover_entries - add daily report columns
-- -----------------------------------------------------------------------------

ALTER TABLE handover_entries
    ADD COLUMN shift VARCHAR(20),
    ADD COLUMN entry_date DATE,
    ADD COLUMN completed_tasks TEXT,
    ADD COLUMN current_progress TEXT,
    ADD COLUMN pending_tasks TEXT,
    ADD COLUMN blockers TEXT,
    ADD COLUMN important_notes TEXT,
    ADD COLUMN estimated_remaining_work VARCHAR(255),
    ADD COLUMN mood VARCHAR(50),
    ADD COLUMN submitted_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_handover_entries_shift ON handover_entries(shift);
CREATE INDEX IF NOT EXISTS idx_handover_entries_entry_date ON handover_entries(entry_date);

-- -----------------------------------------------------------------------------
-- 3) handover_journals - add generation metadata
-- -----------------------------------------------------------------------------

ALTER TABLE handover_journals
    ADD COLUMN shift VARCHAR(20),
    ADD COLUMN journal_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN generated_by VARCHAR(255),
    ADD COLUMN departments_included VARCHAR(500),
    ADD COLUMN entries_count BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_handover_journals_shift ON handover_journals(shift);
