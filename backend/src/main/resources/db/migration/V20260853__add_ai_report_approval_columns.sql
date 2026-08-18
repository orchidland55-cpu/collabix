-- =========================================
-- Collabix AI Report / Handover Approval columns
-- Version 20260853
-- Add approval_status, approved_by, approved_at to the AI analytics and
-- handover journal tables so approve/reject persists the approval action
-- instead of corrupting the AI generation status.
-- =========================================

ALTER TABLE analytics_reports
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS approved_by UUID,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE handover_journals
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS approved_by UUID,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
