-- =========================================
-- Collabix Authorization Sprint
-- Version 20260836
-- Adds missing REPORT_CREATE/UPDATE/READ
-- Grants WORKSPACE_READ to MEMBER role
-- =========================================

-- =========================================
-- PART 1: INSERT MISSING PERMISSIONS
-- =========================================
INSERT INTO permissions (code, display_name, description, created_at, version)
VALUES
    ('REPORT_CREATE', 'Create Report', 'Allows creating reports using AI', NOW(), 0),
    ('REPORT_UPDATE', 'Update Report', 'Allows updating reports', NOW(), 0),
    ('REPORT_READ', 'Read Report', 'Allows viewing reports', NOW(), 0)
ON CONFLICT (code) DO NOTHING;

-- =========================================
-- PART 2: ASSIGN NEW PERMISSIONS TO ROLES
-- =========================================

-- ADMIN gets REPORT_CREATE, REPORT_UPDATE, REPORT_READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('REPORT_CREATE', 'REPORT_UPDATE', 'REPORT_READ')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- MANAGER gets REPORT_CREATE, REPORT_UPDATE, REPORT_READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN ('REPORT_CREATE', 'REPORT_UPDATE', 'REPORT_READ')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- =========================================
-- PART 3: GRANT WORKSPACE_READ TO MEMBER
-- =========================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code = 'WORKSPACE_READ'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
