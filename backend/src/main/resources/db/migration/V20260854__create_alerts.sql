-- =========================================
-- Collabix Alerts Module
-- Version 20260854
-- Create the alerts table and grant alert permissions to roles.
--
-- Alerts are a separate module from Notifications:
--   * Notifications communicate normal events ("task assigned to you").
--   * Alerts represent events that require attention ("task is overdue").
-- Each alert is addressed to exactly one recipient user within one workspace
-- and is optionally scoped to a department. Idempotency is enforced with a
-- nullable dedup_key column under a partial unique index, so scheduled scans
-- never create duplicate alerts for the same event.
-- =========================================

CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Mandatory context (tenant + recipient isolation)
    workspace_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    department_id UUID,

    -- Alert classification
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,

    -- Alert content
    title VARCHAR(255) NOT NULL,
    message VARCHAR(2000),

    -- Optional generic resource reference (TASK, PROJECT, DOCUMENT, REPORT, HANDOVER, ...)
    resource_type VARCHAR(50),
    resource_id UUID,

    -- Idempotency key (nullable; unique only when present)
    dedup_key VARCHAR(255),

    -- Read tracking & lifecycle
    read_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',

    -- AuditableEntity
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    -- Foreign keys
    CONSTRAINT fk_alerts_workspace
        FOREIGN KEY (workspace_id)
            REFERENCES workspaces(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_alerts_recipient
        FOREIGN KEY (recipient_id)
            REFERENCES users(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_alerts_department
        FOREIGN KEY (department_id)
            REFERENCES departments(id)
            ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_alerts_workspace_id ON alerts(workspace_id);
CREATE INDEX idx_alerts_recipient_id ON alerts(recipient_id);
CREATE INDEX idx_alerts_recipient_status ON alerts(recipient_id, status);
CREATE INDEX idx_alerts_recipient_created ON alerts(recipient_id, created_at);
CREATE INDEX idx_alerts_recipient_unread ON alerts(recipient_id, status, read_at);
CREATE INDEX idx_alerts_type ON alerts(type);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_department_id ON alerts(department_id);
CREATE INDEX idx_alerts_resource_type_id ON alerts(resource_type, resource_id);

-- Partial unique index enforces idempotent alert creation for event-scoped
-- alerts (dedup_key is null for one-off system alerts that may repeat).
CREATE UNIQUE INDEX uq_alerts_dedup_key ON alerts(dedup_key) WHERE dedup_key IS NOT NULL;

-- =========================================
-- Permissions
-- =========================================

INSERT INTO permissions (code, display_name, description, created_at, version)
VALUES
    ('ALERT_READ', 'Read Alerts', 'Allows viewing alerts', NOW(), 0),
    ('ALERT_UPDATE', 'Update Alerts', 'Allows updating alerts (mark read)', NOW(), 0),
    ('ALERT_DELETE', 'Delete Alerts', 'Allows dismissing/deleting alerts', NOW(), 0);

-- ADMIN gets all alert permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('ALERT_READ', 'ALERT_UPDATE', 'ALERT_DELETE');

-- MANAGER gets all alert permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN ('ALERT_READ', 'ALERT_UPDATE', 'ALERT_DELETE');

-- MEMBER gets all alert permissions (alerts are always scoped to the
-- authenticated user, so ownership is enforced in the service layer).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code IN ('ALERT_READ', 'ALERT_UPDATE', 'ALERT_DELETE');
