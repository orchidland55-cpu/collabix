-- =========================================
-- Collabix Member AI chat access
-- Version 20260856
-- AI conversations are backed by workspace
-- conversations, so MEMBER needs
-- CONVERSATION_CREATE to start an AI chat
-- (MESSAGE_* were granted previously).
-- =========================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code = 'CONVERSATION_CREATE'
ON CONFLICT (role_id, permission_id) DO NOTHING;
