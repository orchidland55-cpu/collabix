-- =========================================
-- Collabix Member Handover write access
-- Version 20260847
-- Grant MEMBER permission to create, edit and
-- submit their own Handover Entries (daily reports).
-- Journal generation/approval remains ADMIN/MANAGER only
-- (enforced in the service layer).
-- =========================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code IN (
    'HANDOVER_CREATE',
    'HANDOVER_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;
