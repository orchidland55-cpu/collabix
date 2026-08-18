-- =========================================
-- Collabix Announcements Module
-- Version 20260818
-- Add announcement permissions and assign to roles
-- =========================================

INSERT INTO permissions (code, display_name, description, created_at, version)
VALUES
    ('ANNOUNCEMENT_CREATE', 'Create Announcement', 'Allows creating announcements', NOW(), 0),
    ('ANNOUNCEMENT_READ', 'Read Announcement', 'Allows viewing announcements', NOW(), 0),
    ('ANNOUNCEMENT_UPDATE', 'Update Announcement', 'Allows updating announcements', NOW(), 0),
    ('ANNOUNCEMENT_DELETE', 'Delete Announcement', 'Allows deleting announcements', NOW(), 0)
ON CONFLICT (code) DO NOTHING;

-- ADMIN gets all announcement permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('ANNOUNCEMENT_CREATE', 'ANNOUNCEMENT_READ', 'ANNOUNCEMENT_UPDATE', 'ANNOUNCEMENT_DELETE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MANAGER gets create and read
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN ('ANNOUNCEMENT_CREATE', 'ANNOUNCEMENT_READ')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MEMBER gets read only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code IN ('ANNOUNCEMENT_READ')
ON CONFLICT (role_id, permission_id) DO NOTHING;
