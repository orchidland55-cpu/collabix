-- Analytics reports can be generated at workspace scope (no department).
-- Make department_id nullable to match the AnalyticsReport entity.
ALTER TABLE analytics_reports ALTER COLUMN department_id DROP NOT NULL;