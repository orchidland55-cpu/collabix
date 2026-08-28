-- =========================================
-- Collabix
-- Version 20260857
-- Enable ON DELETE CASCADE for remaining project-referencing tables
-- so that a hard project delete cleanly removes its children.
-- (Cascade only fires on an actual row DELETE, so the existing
--  soft-delete / archive behaviour is unaffected.)
-- =========================================

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT tc.table_name, tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu
            ON tc.constraint_name = ccu.constraint_name
        WHERE tc.constraint_type = 'FOREIGN KEY'
          AND ccu.table_name = 'projects'
          AND tc.table_name IN (
              'dev_sprints',
              'analytics_reports',
              'executive_reports',
              'handover_journals'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I DROP CONSTRAINT %I',
            r.table_name, r.constraint_name
        );
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE',
            r.table_name, r.constraint_name
        );
    END LOOP;
END $$;
