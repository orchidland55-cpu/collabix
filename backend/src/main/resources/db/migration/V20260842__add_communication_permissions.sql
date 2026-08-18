INSERT INTO permissions (code, display_name, description, created_at, version)
VALUES
    ('CONVERSATION_CREATE', 'Create Conversation', 'Allows creating conversations/channels', NOW(), 0),
    ('CONVERSATION_READ', 'Read Conversation', 'Allows viewing conversations', NOW(), 0),
    ('CONVERSATION_UPDATE', 'Update Conversation', 'Allows updating conversations', NOW(), 0),
    ('CONVERSATION_DELETE', 'Delete Conversation', 'Allows deleting conversations', NOW(), 0),
    ('MESSAGE_CREATE', 'Create Message', 'Allows sending messages', NOW(), 0),
    ('MESSAGE_READ', 'Read Message', 'Allows reading messages', NOW(), 0),
    ('MESSAGE_UPDATE', 'Update Message', 'Allows editing messages', NOW(), 0),
    ('MESSAGE_DELETE', 'Delete Message', 'Allows deleting messages', NOW(), 0)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('CONVERSATION_CREATE', 'CONVERSATION_READ', 'CONVERSATION_UPDATE', 'CONVERSATION_DELETE',
                 'MESSAGE_CREATE', 'MESSAGE_READ', 'MESSAGE_UPDATE', 'MESSAGE_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN ('CONVERSATION_CREATE', 'CONVERSATION_READ', 'CONVERSATION_UPDATE',
                 'MESSAGE_CREATE', 'MESSAGE_READ', 'MESSAGE_UPDATE', 'MESSAGE_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code IN ('CONVERSATION_READ', 'MESSAGE_CREATE', 'MESSAGE_READ', 'MESSAGE_UPDATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
