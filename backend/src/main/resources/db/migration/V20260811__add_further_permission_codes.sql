-- =========================================
-- Collabix Authorization Sprint - Part 2
-- Version 20260811
-- Missing Permission Codes
-- =========================================

-- =========================================
-- PART 1: INSERT MISSING PERMISSIONS
-- =========================================
INSERT INTO permissions (code, display_name, description, created_at, version)
VALUES
    -- Team Member (completing CRUD)
    ('TEAM_MEMBER_READ', 'Read Team Member', 'Allows viewing team members', NOW(), 0),
    ('TEAM_MEMBER_UPDATE', 'Update Team Member', 'Allows updating team members', NOW(), 0),

    -- Activity (completing CRUD)
    ('ACTIVITY_CREATE', 'Create Activity', 'Allows creating activity logs', NOW(), 0),
    ('ACTIVITY_UPDATE', 'Update Activity', 'Allows updating activity logs', NOW(), 0),
    ('ACTIVITY_DELETE', 'Delete Activity', 'Allows deleting activity logs', NOW(), 0),

    -- Mention (completing CRUD)
    ('MENTION_UPDATE', 'Update Mention', 'Allows updating mentions', NOW(), 0),

    -- Candidate Attachment (completing CRUD)
    ('CANDIDATE_ATTACHMENT_UPDATE', 'Update Candidate Attachment', 'Allows updating candidate attachments', NOW(), 0)
ON CONFLICT (code) DO NOTHING;

-- =========================================
-- PART 2: ASSIGN PERMISSIONS TO ROLES
-- =========================================

-- ADMIN gets all new permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN (
    'TEAM_MEMBER_READ', 'TEAM_MEMBER_UPDATE',
    'ACTIVITY_CREATE', 'ACTIVITY_UPDATE', 'ACTIVITY_DELETE',
    'MENTION_UPDATE',
    'CANDIDATE_ATTACHMENT_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MANAGER gets appropriate permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN (
    'TEAM_MEMBER_READ',
    'ACTIVITY_CREATE', 'ACTIVITY_UPDATE', 'ACTIVITY_DELETE',
    'MENTION_UPDATE',
    'CANDIDATE_ATTACHMENT_UPDATE'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =========================================
-- PART 3: SUPER_ADMIN ROLE
-- =========================================

-- Create SUPER_ADMIN role
INSERT INTO roles (name, description, created_at, version)
SELECT 'SUPER_ADMIN', 'Platform super administrator with cross-workspace access', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SUPER_ADMIN');

-- SUPER_ADMIN gets ALL permissions (cross-workspace bypass is handled by WorkspaceAuthorization isSuperAdmin())
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
