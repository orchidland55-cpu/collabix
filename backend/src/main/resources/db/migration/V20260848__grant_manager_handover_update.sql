-- =========================================
-- Collabix Manager Handover write access
-- Version 20260848
-- Grant MANAGER permission to update/submit/send
-- their own and their department's Handover Entries.
-- (Create + Read were already granted in V20260810.)
-- =========================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code = 'HANDOVER_UPDATE'
ON CONFLICT (role_id, permission_id) DO NOTHING;
