-- =========================================
-- Collabix Authorization Sprint
-- Version 20260814
-- Grant MEMBER role communication permissions
-- =========================================
--
-- The architecture specifies that Employees (MEMBERs) must be able to:
-- - Create messages (comments)
-- - Reply to messages
-- - Mention users
-- - Read their notifications
-- - Read comments in their workspace
--
-- These permissions were missing from V20260810.

-- Grant MEMBER communication permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code IN (
    'COMMENT_CREATE',
    'COMMENT_READ',
    'MENTION_CREATE',
    'MENTION_READ',
    'NOTIFICATION_READ',
    'NOTIFICATION_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;
