-- =========================================
-- Collabix
-- Version 1
-- Initial Migration
-- =========================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE permissions (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL UNIQUE,

    display_name VARCHAR(100) NOT NULL,

    description VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ,

    created_by UUID,

     updated_by UUID,

     version BIGINT NOT NULL DEFAULT 0

);
CREATE TABLE roles (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(50) NOT NULL UNIQUE,

    description VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ,

    created_by UUID,

    updated_by UUID,

    version BIGINT NOT NULL DEFAULT 0

);

CREATE TABLE users (

   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(150) NOT NULL UNIQUE,

    password VARCHAR(255) NOT NULL,

    member_type VARCHAR(20) NOT NULL,

    status VARCHAR(20) NOT NULL,

    profile_picture VARCHAR(255),

    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ,

     created_by UUID,

     updated_by UUID,

      version BIGINT NOT NULL DEFAULT 0

);
CREATE TABLE role_permissions (

   role_id UUID NOT NULL,

    permission_id UUID NOT NULL,

     PRIMARY KEY (role_id, permission_id),

      CONSTRAINT fk_role_permissions_role
          FOREIGN KEY (role_id)
            REFERENCES roles(id)
            ON DELETE CASCADE,

      CONSTRAINT fk_role_permissions_permission
            FOREIGN KEY (permission_id)
               REFERENCES permissions(id)
                ON DELETE CASCADE

);
CREATE TABLE user_roles (

                            user_id UUID NOT NULL,

                            role_id UUID NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE CASCADE

);

CREATE TABLE refresh_tokens (

                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                token VARCHAR(500) NOT NULL UNIQUE,

                                expires_at TIMESTAMPTZ NOT NULL,

                                revoked BOOLEAN NOT NULL DEFAULT FALSE,

                                user_id UUID NOT NULL,

                                created_at TIMESTAMPTZ NOT NULL,

                                updated_at TIMESTAMPTZ,

                                created_by UUID,

                                updated_by UUID,

                                version BIGINT NOT NULL DEFAULT 0,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE

);

INSERT INTO roles (
    name,
    description,
    created_at,
    version
)
VALUES
    (
        'ADMIN',
        'System administrator with full access',
        NOW(),
        0
    ),
    (
        'MANAGER',
        'Team manager',
        NOW(),
        0
    ),
    (
        'MEMBER',
        'Employee or intern',
        NOW(),
        0
    );

INSERT INTO permissions (
    code,
    display_name,
    description,
    created_at,
    version
)
VALUES

    (
        'USER_CREATE',
        'Create User',
        'Allows creating new users',
        NOW(),
        0
    ),

    (
        'USER_READ',
        'Read Users',
        'Allows viewing users',
        NOW(),
        0
    ),

    (
        'USER_UPDATE',
        'Update User',
        'Allows updating users',
        NOW(),
        0
    ),

    (
        'USER_DELETE',
        'Delete User',
        'Allows deleting users',
        NOW(),
        0
    ),

    (
        'ROLE_READ',
        'Read Roles',
        'Allows viewing roles',
        NOW(),
        0
    ),

    (
        'ROLE_UPDATE',
        'Update Roles',
        'Allows updating roles',
        NOW(),
        0
    ),

    (
        'PERMISSION_READ',
        'Read Permissions',
        'Allows viewing permissions',
        NOW(),
        0
    ),

    (
        'ORGANIZATION_READ',
        'Read Organization Structure',
        'Allows viewing departments, teams, and team members',
        NOW(),
        0
    ),

    (
        'ORGANIZATION_WRITE',
        'Manage Organization Structure',
        'Allows creating, updating, and deleting departments, teams, and team members',
        NOW(),
        0
    );

/* ===========================
   ADMIN PERMISSIONS
   =========================== */

INSERT INTO role_permissions (role_id, permission_id)

SELECT
    r.id,
    p.id
FROM roles r
         JOIN permissions p ON TRUE
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)

SELECT
    r.id,
    p.id
FROM roles r
         JOIN permissions p
              ON p.code IN
                 (
                  'USER_READ',
                  'USER_UPDATE'
                     )
WHERE r.name='MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)

SELECT
    r.id,
    p.id
FROM roles r
         JOIN permissions p
              ON p.code IN
                 (
                  'ORGANIZATION_READ',
                  'ORGANIZATION_WRITE'
                     )
WHERE r.name='ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)

SELECT
    r.id,
    p.id
FROM roles r
         JOIN permissions p
              ON p.code IN
                 (
                  'ORGANIZATION_READ'
                     )
WHERE r.name='MANAGER';

/* ===========================================
   INDEXES
   =========================================== */

CREATE INDEX idx_user_roles_role
    ON user_roles(role_id);

CREATE INDEX idx_role_permissions_permission
    ON role_permissions(permission_id);

CREATE INDEX idx_refresh_tokens_user
    ON refresh_tokens(user_id);