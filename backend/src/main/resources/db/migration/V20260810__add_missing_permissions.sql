-- =========================================
-- Collabix Authorization Sprint
-- Version 20260810
-- Complete Permission Catalog
-- =========================================

-- =========================================
-- PART 1: INSERT MISSING PERMISSIONS
-- =========================================
INSERT INTO permissions (code, display_name, description, created_at, version)
VALUES
    -- User Lifecycle
    ('USER_ACTIVATE', 'Activate User', 'Allows activating a user account', NOW(), 0),
    ('USER_DEACTIVATE', 'Deactivate User', 'Allows deactivating a user account', NOW(), 0),
    ('USER_SUSPEND', 'Suspend User', 'Allows suspending a user account', NOW(), 0),
    ('USER_REACTIVATE', 'Reactivate User', 'Allows reactivating a user account', NOW(), 0),
    ('USER_ARCHIVE', 'Archive User', 'Allows archiving a user account', NOW(), 0),
    ('USER_RESTORE', 'Restore User', 'Allows restoring an archived user account', NOW(), 0),

    -- Role Management
    ('ROLE_CREATE', 'Create Role', 'Allows creating new roles', NOW(), 0),
    ('ROLE_DELETE', 'Delete Role', 'Allows deleting roles', NOW(), 0),

    -- Permission Management
    ('PERMISSION_CREATE', 'Create Permission', 'Allows creating new permissions', NOW(), 0),
    ('PERMISSION_UPDATE', 'Update Permission', 'Allows updating permissions', NOW(), 0),
    ('PERMISSION_DELETE', 'Delete Permission', 'Allows deleting permissions', NOW(), 0),

    -- Workspace
    ('WORKSPACE_CREATE', 'Create Workspace', 'Allows creating workspaces', NOW(), 0),
    ('WORKSPACE_READ', 'Read Workspace', 'Allows viewing workspaces', NOW(), 0),
    ('WORKSPACE_UPDATE', 'Update Workspace', 'Allows updating workspaces', NOW(), 0),
    ('WORKSPACE_DELETE', 'Delete Workspace', 'Allows deleting workspaces', NOW(), 0),

    -- Department (replaces generic ORGANIZATION_READ/ORGANIZATION_WRITE)
    ('DEPARTMENT_CREATE', 'Create Department', 'Allows creating departments', NOW(), 0),
    ('DEPARTMENT_READ', 'Read Department', 'Allows viewing departments', NOW(), 0),
    ('DEPARTMENT_UPDATE', 'Update Department', 'Allows updating departments', NOW(), 0),
    ('DEPARTMENT_DELETE', 'Delete Department', 'Allows deleting departments', NOW(), 0),

    -- Team
    ('TEAM_CREATE', 'Create Team', 'Allows creating teams', NOW(), 0),
    ('TEAM_READ', 'Read Team', 'Allows viewing teams', NOW(), 0),
    ('TEAM_UPDATE', 'Update Team', 'Allows updating teams', NOW(), 0),
    ('TEAM_DELETE', 'Delete Team', 'Allows deleting teams', NOW(), 0),

    -- Team Member
    ('TEAM_MEMBER_ADD', 'Add Team Member', 'Allows adding members to teams', NOW(), 0),
    ('TEAM_MEMBER_REMOVE', 'Remove Team Member', 'Allows removing members from teams', NOW(), 0),

    -- Project
    ('PROJECT_CREATE', 'Create Project', 'Allows creating projects', NOW(), 0),
    ('PROJECT_READ', 'Read Project', 'Allows viewing projects', NOW(), 0),
    ('PROJECT_UPDATE', 'Update Project', 'Allows updating projects', NOW(), 0),
    ('PROJECT_DELETE', 'Delete Project', 'Allows deleting projects', NOW(), 0),

    -- Task
    ('TASK_CREATE', 'Create Task', 'Allows creating tasks', NOW(), 0),
    ('TASK_READ', 'Read Task', 'Allows viewing tasks', NOW(), 0),
    ('TASK_UPDATE', 'Update Task', 'Allows updating tasks', NOW(), 0),
    ('TASK_DELETE', 'Delete Task', 'Allows deleting tasks', NOW(), 0),
    ('TASK_ASSIGN', 'Assign Task', 'Allows assigning tasks to users', NOW(), 0),

    -- Comment
    ('COMMENT_CREATE', 'Create Comment', 'Allows creating comments', NOW(), 0),
    ('COMMENT_READ', 'Read Comment', 'Allows viewing comments', NOW(), 0),
    ('COMMENT_UPDATE', 'Update Comment', 'Allows updating comments', NOW(), 0),
    ('COMMENT_DELETE', 'Delete Comment', 'Allows deleting comments', NOW(), 0),

    -- Document
    ('DOCUMENT_UPLOAD', 'Upload Document', 'Allows uploading documents', NOW(), 0),
    ('DOCUMENT_READ', 'Read Document', 'Allows viewing documents', NOW(), 0),
    ('DOCUMENT_UPDATE', 'Update Document', 'Allows updating documents', NOW(), 0),
    ('DOCUMENT_DELETE', 'Delete Document', 'Allows deleting documents', NOW(), 0),

    -- Knowledge Base
    ('KNOWLEDGE_BASE_CREATE', 'Create Knowledge Base Article', 'Allows creating knowledge base articles', NOW(), 0),
    ('KNOWLEDGE_BASE_READ', 'Read Knowledge Base Article', 'Allows viewing knowledge base articles', NOW(), 0),
    ('KNOWLEDGE_BASE_UPDATE', 'Update Knowledge Base Article', 'Allows updating knowledge base articles', NOW(), 0),
    ('KNOWLEDGE_BASE_DELETE', 'Delete Knowledge Base Article', 'Allows deleting knowledge base articles', NOW(), 0),

    -- Attachment
    ('ATTACHMENT_UPLOAD', 'Upload Attachment', 'Allows uploading attachments', NOW(), 0),
    ('ATTACHMENT_READ', 'Read Attachment', 'Allows viewing attachments', NOW(), 0),
    ('ATTACHMENT_UPDATE', 'Update Attachment', 'Allows updating attachments', NOW(), 0),
    ('ATTACHMENT_DELETE', 'Delete Attachment', 'Allows deleting attachments', NOW(), 0),

    -- Activity
    ('ACTIVITY_READ', 'Read Activity', 'Allows viewing activity logs', NOW(), 0),

    -- Notification
    ('NOTIFICATION_READ', 'Read Notification', 'Allows viewing notifications', NOW(), 0),
    ('NOTIFICATION_UPDATE', 'Update Notification', 'Allows updating notifications', NOW(), 0),
    ('NOTIFICATION_DELETE', 'Delete Notification', 'Allows deleting notifications', NOW(), 0),

    -- Mention
    ('MENTION_CREATE', 'Create Mention', 'Allows creating mentions', NOW(), 0),
    ('MENTION_READ', 'Read Mention', 'Allows viewing mentions', NOW(), 0),
    ('MENTION_DELETE', 'Delete Mention', 'Allows deleting mentions', NOW(), 0),

    -- Handover
    ('HANDOVER_CREATE', 'Create Handover', 'Allows creating handover journals', NOW(), 0),
    ('HANDOVER_READ', 'Read Handover', 'Allows viewing handovers', NOW(), 0),
    ('HANDOVER_UPDATE', 'Update Handover', 'Allows updating handovers', NOW(), 0),
    ('HANDOVER_DELETE', 'Delete Handover', 'Allows deleting handovers', NOW(), 0),
    ('HANDOVER_APPROVE', 'Approve Handover', 'Allows approving handovers', NOW(), 0),
    ('HANDOVER_ENTRY_CREATE', 'Create Handover Entry', 'Allows creating handover entries', NOW(), 0),
    ('HANDOVER_ENTRY_READ', 'Read Handover Entry', 'Allows viewing handover entries', NOW(), 0),
    ('HANDOVER_ENTRY_UPDATE', 'Update Handover Entry', 'Allows updating handover entries', NOW(), 0),
    ('HANDOVER_ENTRY_DELETE', 'Delete Handover Entry', 'Allows deleting handover entries', NOW(), 0),

    -- Dashboard
    ('DASHBOARD_VIEW', 'View Dashboard', 'Allows viewing dashboards', NOW(), 0),

    -- HR: Employee
    ('EMPLOYEE_CREATE', 'Create Employee', 'Allows creating employees', NOW(), 0),
    ('EMPLOYEE_READ', 'Read Employee', 'Allows viewing employees', NOW(), 0),
    ('EMPLOYEE_UPDATE', 'Update Employee', 'Allows updating employees', NOW(), 0),
    ('EMPLOYEE_DELETE', 'Delete Employee', 'Allows deleting employees', NOW(), 0),

    -- HR: Candidate
    ('CANDIDATE_CREATE', 'Create Candidate', 'Allows creating candidates', NOW(), 0),
    ('CANDIDATE_READ', 'Read Candidate', 'Allows viewing candidates', NOW(), 0),
    ('CANDIDATE_UPDATE', 'Update Candidate', 'Allows updating candidates', NOW(), 0),
    ('CANDIDATE_DELETE', 'Delete Candidate', 'Allows deleting candidates', NOW(), 0),

    -- HR: Interview
    ('INTERVIEW_CREATE', 'Create Interview', 'Allows creating interviews', NOW(), 0),
    ('INTERVIEW_READ', 'Read Interview', 'Allows viewing interviews', NOW(), 0),
    ('INTERVIEW_UPDATE', 'Update Interview', 'Allows updating interviews', NOW(), 0),
    ('INTERVIEW_DELETE', 'Delete Interview', 'Allows deleting interviews', NOW(), 0),
    ('INTERVIEW_CANCEL', 'Cancel Interview', 'Allows cancelling interviews', NOW(), 0),

    -- HR: Interview Calendar
    ('INTERVIEW_CALENDAR_VIEW', 'View Interview Calendar', 'Allows viewing interview calendars', NOW(), 0),

    -- HR: Onboarding
    ('ONBOARDING_CREATE', 'Create Onboarding', 'Allows creating onboarding plans', NOW(), 0),
    ('ONBOARDING_READ', 'Read Onboarding', 'Allows viewing onboarding plans', NOW(), 0),
    ('ONBOARDING_UPDATE', 'Update Onboarding', 'Allows updating onboarding plans', NOW(), 0),
    ('ONBOARDING_DELETE', 'Delete Onboarding', 'Allows deleting onboarding plans', NOW(), 0),
    ('ONBOARDING_TASK_MANAGE', 'Manage Onboarding Tasks', 'Allows managing onboarding tasks', NOW(), 0),

    -- HR: Performance Review
    ('PERFORMANCE_REVIEW_CREATE', 'Create Performance Review', 'Allows creating performance reviews', NOW(), 0),
    ('PERFORMANCE_REVIEW_READ', 'Read Performance Review', 'Allows viewing performance reviews', NOW(), 0),
    ('PERFORMANCE_REVIEW_UPDATE', 'Update Performance Review', 'Allows updating performance reviews', NOW(), 0),
    ('PERFORMANCE_REVIEW_DELETE', 'Delete Performance Review', 'Allows deleting performance reviews', NOW(), 0),
    ('PERFORMANCE_REVIEW_SUBMIT', 'Submit Performance Review', 'Allows submitting performance reviews', NOW(), 0),
    ('PERFORMANCE_REVIEW_APPROVE', 'Approve Performance Review', 'Allows approving performance reviews', NOW(), 0),

    -- HR: Attendance
    ('ATTENDANCE_CREATE', 'Create Attendance Record', 'Allows creating attendance records', NOW(), 0),
    ('ATTENDANCE_READ', 'Read Attendance', 'Allows viewing attendance records', NOW(), 0),
    ('ATTENDANCE_UPDATE', 'Update Attendance', 'Allows updating attendance records', NOW(), 0),
    ('ATTENDANCE_DELETE', 'Delete Attendance', 'Allows deleting attendance records', NOW(), 0),

    -- HR: Employee Document
    ('EMPLOYEE_DOCUMENT_UPLOAD', 'Upload Employee Document', 'Allows uploading employee documents', NOW(), 0),
    ('EMPLOYEE_DOCUMENT_READ', 'Read Employee Document', 'Allows viewing employee documents', NOW(), 0),
    ('EMPLOYEE_DOCUMENT_UPDATE', 'Update Employee Document', 'Allows updating employee documents', NOW(), 0),
    ('EMPLOYEE_DOCUMENT_DELETE', 'Delete Employee Document', 'Allows deleting employee documents', NOW(), 0),
    ('EMPLOYEE_DOCUMENT_VERIFY', 'Verify Employee Document', 'Allows verifying employee documents', NOW(), 0),

    -- HR: Recruiter Note
    ('RECRUITER_NOTE_CREATE', 'Create Recruiter Note', 'Allows creating recruiter notes', NOW(), 0),
    ('RECRUITER_NOTE_READ', 'Read Recruiter Note', 'Allows viewing recruiter notes', NOW(), 0),
    ('RECRUITER_NOTE_UPDATE', 'Update Recruiter Note', 'Allows updating recruiter notes', NOW(), 0),
    ('RECRUITER_NOTE_DELETE', 'Delete Recruiter Note', 'Allows deleting recruiter notes', NOW(), 0),

    -- HR: Employee Skill
    ('EMPLOYEE_SKILL_CREATE', 'Create Employee Skill', 'Allows creating employee skills', NOW(), 0),
    ('EMPLOYEE_SKILL_READ', 'Read Employee Skill', 'Allows viewing employee skills', NOW(), 0),
    ('EMPLOYEE_SKILL_UPDATE', 'Update Employee Skill', 'Allows updating employee skills', NOW(), 0),
    ('EMPLOYEE_SKILL_DELETE', 'Delete Employee Skill', 'Allows deleting employee skills', NOW(), 0),

    -- HR: Notification
    ('HR_NOTIFICATION_READ', 'Read HR Notification', 'Allows viewing HR notifications', NOW(), 0),
    ('HR_NOTIFICATION_DISMISS', 'Dismiss HR Notification', 'Allows dismissing HR notifications', NOW(), 0),

    -- HR: Candidate Attachment
    ('CANDIDATE_ATTACHMENT_UPLOAD', 'Upload Candidate Attachment', 'Allows uploading candidate attachments', NOW(), 0),
    ('CANDIDATE_ATTACHMENT_READ', 'Read Candidate Attachment', 'Allows viewing candidate attachments', NOW(), 0),
    ('CANDIDATE_ATTACHMENT_DELETE', 'Delete Candidate Attachment', 'Allows deleting candidate attachments', NOW(), 0),

    -- Marketing: Campaign
    ('CAMPAIGN_CREATE', 'Create Campaign', 'Allows creating marketing campaigns', NOW(), 0),
    ('CAMPAIGN_READ', 'Read Campaign', 'Allows viewing marketing campaigns', NOW(), 0),
    ('CAMPAIGN_UPDATE', 'Update Campaign', 'Allows updating marketing campaigns', NOW(), 0),
    ('CAMPAIGN_ACTIVATE', 'Activate Campaign', 'Allows activating marketing campaigns', NOW(), 0),
    ('CAMPAIGN_COMPLETE', 'Complete Campaign', 'Allows completing marketing campaigns', NOW(), 0),
    ('CAMPAIGN_ARCHIVE', 'Archive Campaign', 'Allows archiving marketing campaigns', NOW(), 0),

    -- Dev: Sprint
    ('SPRINT_CREATE', 'Create Sprint', 'Allows creating sprints', NOW(), 0),
    ('SPRINT_READ', 'Read Sprint', 'Allows viewing sprints', NOW(), 0),
    ('SPRINT_UPDATE', 'Update Sprint', 'Allows updating sprints', NOW(), 0),
    ('SPRINT_DELETE', 'Delete Sprint', 'Allows deleting sprints', NOW(), 0),
    ('SPRINT_ACTIVATE', 'Activate Sprint', 'Allows activating sprints', NOW(), 0),
    ('SPRINT_COMPLETE', 'Complete Sprint', 'Allows completing sprints', NOW(), 0),
    ('SPRINT_ARCHIVE', 'Archive Sprint', 'Allows archiving sprints', NOW(), 0),

    -- Cybersecurity: Security Audit
    ('SECURITY_AUDIT_CREATE', 'Create Security Audit', 'Allows creating security audits', NOW(), 0),
    ('SECURITY_AUDIT_READ', 'Read Security Audit', 'Allows viewing security audits', NOW(), 0),
    ('SECURITY_AUDIT_UPDATE', 'Update Security Audit', 'Allows updating security audits', NOW(), 0),
    ('SECURITY_AUDIT_START', 'Start Security Audit', 'Allows starting security audits', NOW(), 0),
    ('SECURITY_AUDIT_COMPLETE', 'Complete Security Audit', 'Allows completing security audits', NOW(), 0),
    ('SECURITY_AUDIT_ARCHIVE', 'Archive Security Audit', 'Allows archiving security audits', NOW(), 0),

    -- AI: Model
    ('AI_MODEL_CREATE', 'Create AI Model', 'Allows creating AI models', NOW(), 0),
    ('AI_MODEL_READ', 'Read AI Model', 'Allows viewing AI models', NOW(), 0),
    ('AI_MODEL_UPDATE', 'Update AI Model', 'Allows updating AI models', NOW(), 0),
    ('AI_MODEL_ARCHIVE', 'Archive AI Model', 'Allows archiving AI models', NOW(), 0),

    -- Reports
    ('REPORT_VIEW', 'View Report', 'Allows viewing reports', NOW(), 0),
    ('REPORT_EXPORT', 'Export Report', 'Allows exporting reports', NOW(), 0),
    ('REPORT_SCHEDULE', 'Schedule Report', 'Allows scheduling reports', NOW(), 0),
    ('REPORT_HISTORY_VIEW', 'View Report History', 'Allows viewing report history', NOW(), 0),

    -- Analytics
    ('ANALYTICS_VIEW', 'View Analytics', 'Allows viewing analytics', NOW(), 0),
    ('ANALYTICS_EXPORT', 'Export Analytics', 'Allows exporting analytics', NOW(), 0),

    -- Admin
    ('ADMIN_USER_UNLOCK', 'Unlock User Account', 'Allows unlocking user accounts', NOW(), 0),

    -- User management: Read/Update/Delete (used by UserController)
    ('USER_READ',  'Read User',   'Allows viewing user accounts', NOW(), 0),
    ('USER_UPDATE', 'Update User', 'Allows updating user accounts', NOW(), 0),
    ('USER_DELETE', 'Delete User', 'Allows deleting user accounts', NOW(), 0),

    -- Role: Read (used by RoleController)
    ('ROLE_READ', 'Read Role', 'Allows viewing roles', NOW(), 0),

    -- Permission: Read (used by PermissionController)
    ('PERMISSION_READ', 'Read Permission', 'Allows viewing permissions', NOW(), 0)
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
    'USER_ACTIVATE', 'USER_DEACTIVATE', 'USER_SUSPEND', 'USER_REACTIVATE',
    'USER_ARCHIVE', 'USER_RESTORE', 'USER_READ', 'USER_UPDATE', 'USER_DELETE',
    'ROLE_CREATE', 'ROLE_READ', 'ROLE_DELETE',
    'PERMISSION_CREATE', 'PERMISSION_READ', 'PERMISSION_UPDATE', 'PERMISSION_DELETE',
    'WORKSPACE_CREATE', 'WORKSPACE_READ', 'WORKSPACE_UPDATE', 'WORKSPACE_DELETE',
    'DEPARTMENT_CREATE', 'DEPARTMENT_READ', 'DEPARTMENT_UPDATE', 'DEPARTMENT_DELETE',
    'TEAM_CREATE', 'TEAM_READ', 'TEAM_UPDATE', 'TEAM_DELETE',
    'TEAM_MEMBER_ADD', 'TEAM_MEMBER_REMOVE',
    'PROJECT_CREATE', 'PROJECT_READ', 'PROJECT_UPDATE', 'PROJECT_DELETE',
    'TASK_CREATE', 'TASK_READ', 'TASK_UPDATE', 'TASK_DELETE', 'TASK_ASSIGN',
    'COMMENT_CREATE', 'COMMENT_READ', 'COMMENT_UPDATE', 'COMMENT_DELETE',
    'DOCUMENT_UPLOAD', 'DOCUMENT_READ', 'DOCUMENT_UPDATE', 'DOCUMENT_DELETE',
    'KNOWLEDGE_BASE_CREATE', 'KNOWLEDGE_BASE_READ', 'KNOWLEDGE_BASE_UPDATE', 'KNOWLEDGE_BASE_DELETE',
    'ATTACHMENT_UPLOAD', 'ATTACHMENT_READ', 'ATTACHMENT_UPDATE', 'ATTACHMENT_DELETE',
    'ACTIVITY_READ',
    'NOTIFICATION_READ', 'NOTIFICATION_UPDATE', 'NOTIFICATION_DELETE',
    'MENTION_CREATE', 'MENTION_READ', 'MENTION_DELETE',
    'HANDOVER_CREATE', 'HANDOVER_READ', 'HANDOVER_UPDATE', 'HANDOVER_DELETE', 'HANDOVER_APPROVE',
    'HANDOVER_ENTRY_CREATE', 'HANDOVER_ENTRY_READ', 'HANDOVER_ENTRY_UPDATE', 'HANDOVER_ENTRY_DELETE',
    'DASHBOARD_VIEW',
    'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DELETE',
    'CANDIDATE_CREATE', 'CANDIDATE_READ', 'CANDIDATE_UPDATE', 'CANDIDATE_DELETE',
    'INTERVIEW_CREATE', 'INTERVIEW_READ', 'INTERVIEW_UPDATE', 'INTERVIEW_DELETE', 'INTERVIEW_CANCEL',
    'INTERVIEW_CALENDAR_VIEW',
    'ONBOARDING_CREATE', 'ONBOARDING_READ', 'ONBOARDING_UPDATE', 'ONBOARDING_DELETE', 'ONBOARDING_TASK_MANAGE',
    'PERFORMANCE_REVIEW_CREATE', 'PERFORMANCE_REVIEW_READ', 'PERFORMANCE_REVIEW_UPDATE', 'PERFORMANCE_REVIEW_DELETE',
    'PERFORMANCE_REVIEW_SUBMIT', 'PERFORMANCE_REVIEW_APPROVE',
    'ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'ATTENDANCE_UPDATE', 'ATTENDANCE_DELETE',
    'EMPLOYEE_DOCUMENT_UPLOAD', 'EMPLOYEE_DOCUMENT_READ', 'EMPLOYEE_DOCUMENT_UPDATE', 'EMPLOYEE_DOCUMENT_DELETE',
    'EMPLOYEE_DOCUMENT_VERIFY',
    'RECRUITER_NOTE_CREATE', 'RECRUITER_NOTE_READ', 'RECRUITER_NOTE_UPDATE', 'RECRUITER_NOTE_DELETE',
    'EMPLOYEE_SKILL_CREATE', 'EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_UPDATE', 'EMPLOYEE_SKILL_DELETE',
    'HR_NOTIFICATION_READ', 'HR_NOTIFICATION_DISMISS',
    'CANDIDATE_ATTACHMENT_UPLOAD', 'CANDIDATE_ATTACHMENT_READ', 'CANDIDATE_ATTACHMENT_DELETE',
    'CAMPAIGN_CREATE', 'CAMPAIGN_READ', 'CAMPAIGN_UPDATE', 'CAMPAIGN_ACTIVATE', 'CAMPAIGN_COMPLETE', 'CAMPAIGN_ARCHIVE',
    'SPRINT_CREATE', 'SPRINT_READ', 'SPRINT_UPDATE', 'SPRINT_DELETE', 'SPRINT_ACTIVATE', 'SPRINT_COMPLETE', 'SPRINT_ARCHIVE',
    'SECURITY_AUDIT_CREATE', 'SECURITY_AUDIT_READ', 'SECURITY_AUDIT_UPDATE', 'SECURITY_AUDIT_START', 'SECURITY_AUDIT_COMPLETE', 'SECURITY_AUDIT_ARCHIVE',
    'AI_MODEL_CREATE', 'AI_MODEL_READ', 'AI_MODEL_UPDATE', 'AI_MODEL_ARCHIVE',
    'REPORT_VIEW', 'REPORT_EXPORT', 'REPORT_SCHEDULE', 'REPORT_HISTORY_VIEW',
    'ANALYTICS_VIEW', 'ANALYTICS_EXPORT',
    'ADMIN_USER_UNLOCK'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MANAGER gets read + moderate write permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN (
    'USER_ACTIVATE', 'USER_DEACTIVATE', 'USER_REACTIVATE',
    'WORKSPACE_READ', 'WORKSPACE_UPDATE',
    'DEPARTMENT_READ', 'DEPARTMENT_UPDATE',
    'TEAM_READ', 'TEAM_UPDATE', 'TEAM_MEMBER_ADD', 'TEAM_MEMBER_REMOVE',
    'PROJECT_READ', 'PROJECT_UPDATE', 'PROJECT_CREATE',
    'TASK_READ', 'TASK_UPDATE', 'TASK_CREATE', 'TASK_ASSIGN',
    'COMMENT_READ', 'COMMENT_CREATE',
    'DOCUMENT_READ', 'DOCUMENT_UPLOAD',
    'KNOWLEDGE_BASE_READ', 'KNOWLEDGE_BASE_CREATE',
    'ATTACHMENT_READ', 'ATTACHMENT_UPLOAD',
    'ACTIVITY_READ',
    'NOTIFICATION_READ', 'NOTIFICATION_UPDATE',
    'MENTION_READ', 'MENTION_CREATE',
    'HANDOVER_READ', 'HANDOVER_CREATE', 'HANDOVER_ENTRY_READ', 'HANDOVER_ENTRY_CREATE',
    'DASHBOARD_VIEW',
    'EMPLOYEE_READ', 'EMPLOYEE_UPDATE', 'EMPLOYEE_CREATE',
    'CANDIDATE_READ', 'CANDIDATE_UPDATE', 'CANDIDATE_CREATE',
    'INTERVIEW_READ', 'INTERVIEW_CREATE', 'INTERVIEW_UPDATE',
    'INTERVIEW_CALENDAR_VIEW',
    'ONBOARDING_READ', 'ONBOARDING_CREATE', 'ONBOARDING_UPDATE', 'ONBOARDING_TASK_MANAGE',
    'PERFORMANCE_REVIEW_READ', 'PERFORMANCE_REVIEW_CREATE', 'PERFORMANCE_REVIEW_UPDATE',
    'PERFORMANCE_REVIEW_SUBMIT',
    'ATTENDANCE_READ', 'ATTENDANCE_CREATE',
    'EMPLOYEE_DOCUMENT_READ', 'EMPLOYEE_DOCUMENT_UPLOAD',
    'RECRUITER_NOTE_READ', 'RECRUITER_NOTE_CREATE',
    'EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_CREATE',
    'HR_NOTIFICATION_READ',
    'CANDIDATE_ATTACHMENT_READ', 'CANDIDATE_ATTACHMENT_UPLOAD',
    'CAMPAIGN_READ', 'CAMPAIGN_CREATE', 'CAMPAIGN_UPDATE', 'CAMPAIGN_ACTIVATE', 'CAMPAIGN_COMPLETE',
    'SPRINT_READ', 'SPRINT_CREATE', 'SPRINT_UPDATE', 'SPRINT_ACTIVATE', 'SPRINT_COMPLETE',
    'SECURITY_AUDIT_READ', 'SECURITY_AUDIT_CREATE', 'SECURITY_AUDIT_UPDATE', 'SECURITY_AUDIT_START',
    'AI_MODEL_READ', 'AI_MODEL_CREATE', 'AI_MODEL_UPDATE',
    'REPORT_VIEW', 'REPORT_EXPORT', 'REPORT_SCHEDULE', 'REPORT_HISTORY_VIEW',
    'ANALYTICS_VIEW'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- MEMBER gets minimal self-service permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.code IN (
    'HANDOVER_ENTRY_CREATE', 'HANDOVER_ENTRY_READ'
)
ON CONFLICT (role_id, permission_id) DO NOTHING;
