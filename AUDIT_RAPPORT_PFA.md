# PROJECT REPORT DATA EXTRACTION — AUDIT TECHNIQUE COLLABIX

> **Nature du document** : source de vérité technique extraite du code réel du repository `C:\Users\SURFACE\Desktop\collabix` (audit statique réalisé le 22/08/2026).
> **Règle appliquée** : CODE RÉEL > DOCUMENTATION ANCIENNE > HYPOTHÈSES. Tout ce qui n'est pas prouvé par le code est marqué `À CONFIRMER AVEC LE STAGIAIRE` ou `NON TROUVÉ DANS LE CODE`.
> **Aucune valeur secrète** (mots de passe, clés API, secrets JWT) n'est reproduite dans ce document.

---

## 1. Project Overview

| Élément | Valeur | Source |
|---|---|---|
| Nom réel du projet | **Collabix** | URLs de production (`collabix-chi.vercel.app`), issuer JWT `app.jwt.issuer=collabix`, base `collabix_db`, fichier `COLLABIX_BACKEND_AUDIT.md` |
| Artefact backend | `com.trio:backend:0.0.1-SNAPSHOT` | `backend/pom.xml` — le groupId `com.trio` suggère une équipe de 3 personnes (`À CONFIRMER AVEC LE STAGIAIRE`) |
| Nom package.json frontend | `vite-react-typescript-starter` v0.0.0 | ⚠️ différent du nom métier Collabix (voir §28 Contradictions) |
| Type d'application | Application web collaborative de gestion d'organisation / workspace (SaaS multi-tenant interne) | Architecture workspace-scoped vérifiée dans les contrôleurs |
| Domaine métier | Collaboration d'équipe : projets, tâches, documents, communication, RH, développement (sprints), cybersécurité (audits), marketing (campagnes), assistant IA | Inventaire des contrôleurs et pages |
| Utilisateurs ciblés | Entreprises/organisations avec départements (RH, Développement, Cybersécurité, Marketing) ; rôles SUPER_ADMIN / ADMIN / MANAGER / MEMBER | `enums/RoleName.java`, pages departments/* |
| Environnement | Web ; frontend déployé sur Vercel (`collabix-chi.vercel.app`), backend sur Railway (`collabix-production-eead.up.railway.app`), DB PostgreSQL managée Railway | `application.properties`, `.vercel/project.json`, fallback CORS |
| Version | Backend `0.0.1-SNAPSHOT` ; aucun tag Git ; frontend `0.0.0` | `pom.xml`, `package.json`, `git tag` (vide) |
| Architecture générale | Monolithe Spring Boot (API REST stateless + WebSocket) + SPA React, PostgreSQL, Flyway | Analyse complète du code |

**Objectif général** (déduit du code uniquement) : centraliser la collaboration d'une organisation — membres, équipes, départements, projets, tâches, documents, passation (handover), communication, notifications temps réel, analytics et assistance IA générative.

---

## 2. Repository Structure

```text
collabix/
├── backend/                  # API Spring Boot (Java 21, Maven)
│   ├── src/main/java/com/trio/backend/
│   │   ├── ai/              # Module IA autonome (controller/dto/entity/service/enums/mapper/repository)
│   │   ├── common/, config/, controller/, dto/, entity/, enums/,
│   │   ├── event/, exception/, mapper/, reporting/analytics/, repository/,
│   │   ├── security/        # jwt, permission, user, workspace, department, audit, ai
│   │   ├── service/, storage/, util/, validation/, websocket/
│   │   └── controller/{ai,cyber,dev,hr,marketing}/   # modules métier par département
│   ├── src/main/resources/
│   │   ├── application{,-dev,-prod}.properties
│   │   ├── db/migration/    # 80 migrations Flyway SQL PostgreSQL
│   │   └── templates/emails/ # Templates Thymeleaf (activation, reset, notifications, HR)
│   ├── src/test/java/com/trio/backend/   # 24 classes (unit + integration H2)
│   ├── Dockerfile           # Multi-stage Maven→JRE21
│   └── pom.xml
├── frontend/project/         # SPA React 18 + TypeScript + Vite 5 + Tailwind
│   ├── src/components/{ui,layout,ai,calendar,documents,file-preview,errors,activity,admin,search,profile}/
│   ├── src/pages/            # auth, projects, tasks, teams, departments (hr/dev/cyber/marketing),
│   │   ├── Administration/   # users, roles, permissions, audit logs
│   │   ├── ai/ productivity/ knowledge/ handover/ communication/ profile/ settings/ workspace/
│   ├── src/services/         # ~45 couples *-service.ts / *-hooks.ts (axios + TanStack Query)
│   ├── src/lib/              # api.ts (axios+interceptors), auth-context.tsx, query-client.ts, theme.tsx…
│   ├── vercel.json           # Rewrite SPA
│   └── package.json
├── database/                 # DOSSIER VIDE (le schéma vit dans les migrations Flyway backend)
├── docs/SECURITY_REPORT.md   # Rapport "Security Sprint 10" — score 84/100
├── diagrams/                 # DOSSIER VIDE
├── .github/modernize/        # Artefacts outil "java-upgrade" — AUCUN workflow CI/CD
├── COLLABIX_BACKEND_AUDIT.md # Audit backend ~600 lignes (auth, autorisation, rôles…)
├── TODO.md                   # Backlog frontend ouvert (Department Experience Polish)
└── scripts utilitaires racine (fix-imports.js, list-any-errors.js…) — outils de dev jetables
```

| Dossier | Rôle |
|---|---|
| `backend/src/main/java/com/trio/backend/controller` | 54 contrôleurs REST (dont sous-packages `hr`, `dev`, `cyber`, `marketing`, `ai`) |
| `backend/.../security` | JWT, permissions, scoping workspace/département, rate limiting |
| `backend/.../db/migration` | Source unique de vérité du schéma (Flyway, PostgreSQL) |
| `frontend/project/src/services` | Couche d'accès API typée (axios) + hooks TanStack Query |
| `docs/` | Uniquement `SECURITY_REPORT.md` |
| `database/`, `diagrams/` | **Vides** — à remplir ou ignorer dans le rapport |

---

## 3. Functional Modules

Légende : ✅ implémenté (front+back+DB), 🟡 partiel, ❌ non implémenté. Sources = fichiers cités.

| Module | Fonctionnalité | Implémentée ? | Frontend | Backend | Base de données | Source |
|---|---|---|---|---|---|---|
| **Authentification** | Login (JWT access+refresh) | ✅ | `LoginPage.tsx`, `auth-context.tsx` | `AuthController POST /api/auth/login` | `users`, `refresh_tokens` | AuthController, JwtService |
| | Refresh token persisté + révocation | ✅ | intercepteur 401 single-flight (`lib/api.ts`) | `POST /api/auth/refresh`, `RefreshTokenServiceImpl` (lock pessimiste) | `refresh_tokens` | idem |
| | Logout (révocation refresh) | ✅ | `signOut()` | `POST /api/auth/logout` | update `revoked` | idem |
| | Mot de passe oublié / reset par token | ✅ | `ForgotPasswordPage`, `pages/reset-password/*` | `POST /forgot-password`, `/reset-password` | `password_reset_tokens` | idem |
| | Activation de compte par email (token) | ✅ | `pages/activate/*` | `ActivationController GET/POST /activate`, `/resend-activation` | `activation_tokens` | idem |
| | Changement de mot de passe | ✅ | `profile/SecurityPage.tsx` | `POST /change-password` | `users.password` (BCrypt 10) | idem |
| | Anti brute-force (5 essais → lock 30 min) | ✅ | message erreur login | `LoginSecurityService`, config `app.login-security.*` | `failed_login_attempts`, `locked_at` (V8) | SecurityConfig |
| **Utilisateurs & Administration** | CRUD utilisateurs + cycle de vie (activate/suspend/archive/restore/delete/permanent delete) | ✅ | `Administration/Users Management/*` | `UserController /api/workspaces/{wsId}/users/**` | `users` (+`user_history` via UserHistoryController) | UserController |
| | Rôles & permissions fines (RBAC custom) | ✅ | `Administration/Roles & Permission Management` | `RoleController`, `PermissionController` | `roles`, `permissions`, `role_permissions`, `user_roles` | migrations V1, V20260810-14… |
| | Audit logs admin | ✅ (lecture) | `Audit Logs/AuditLogsPage.tsx` | entités `Activity`/`UserHistory` | `activities`, `user_history` | pages/Administration |
| | Déverrouillage de compte | ✅ | modal users | `AdminUserController POST /{userId}/unlock` | `locked_at` | AdminUserController |
| **Workspace / Organisation** | Multi-workspaces (créer/archiver/restaurer/supprimer, membres OWNER/ADMIN/MEMBER) | ✅ | `workspace/*`, `CreateWorkspacePage`… | `WorkspaceController /api/workspaces` | `workspaces`, `workspace_members` | WorkspaceController |
| | Départements (CRUD, soft/hard delete, scoping) | ✅ | `departments/*`, `OrganizationPage` | `DepartmentController` | `departments` | DepartmentController |
| | Équipes + membres d'équipe | ✅ | `teams/*` | `TeamController`, `TeamMemberController` | `teams`, `team_members` | TeamController |
| | Membres / annuaire | ✅ | `members/*` | `UserController /search` | `users` | MembersPage |
| **Projets** | CRUD projets + archivage/restauration + couleur/icône/priorités/dates | ✅ | `projects/*` + modals | `ProjectController`, `WorkspaceProjectController` | `projects` (V8_1, V20260839) | ProjectController |
| **Tâches** | CRUD tâches, filtres, pagination, kanban workflow | ✅ | `tasks/TasksPage`, `TaskDetailsPage` | `TaskController` (`findFiltered` paginé) | `tasks` (status/priority/storyPoints/sprint FK) | TaskController, TaskRepository |
| | Checklists + items (toggle) | ✅ | TaskDetailsPage | `ChecklistController` | `checklists`, `checklist_items` (V20260840) | ChecklistController |
| | Commentaires (+ pièces jointes, mentions) | ✅ | TaskDetailsPage, CollaborationPage | `CommentController`, `MentionController`, `AttachmentController` | `comments`, `mentions`, `attachments` | CommentController |
| | Activités liées aux tâches | ✅ | `activity/*` | `ActivityController` | `activities` (V20260832) | ActivityController |
| **Documents** | Upload/download multipart, versioning, approbation (submit/approve/reject), tags, catégories, soft archive | ✅ | `knowledge/DocumentsPage`, `file-preview/*` | `DocumentController` (+upload `/upload`, `/download`) | `documents`, `document_tags`, `tags`, `version_history` (V20260820-22) | DocumentController |
| | Base de connaissances (articles, catégories, approbation, versions) | ✅ | `knowledge/KnowledgeBasePage` | `KnowledgeBaseController` | `knowledge_bases` (fulltext GIN V20260823) | KnowledgeBaseController |
| **Handover (passation)** | Entrées de passation (workflow send/accept/reject/complete), inbox/sent, commentaires, PJ, timeline | ✅ | `handover/*` | `HandoverEntryController /api/workspaces/{wsId}/handovers` | `handover_entries`(+attachments/comments/timeline_events) — seul flag `deleted` explicite | HandoverEntryController |
| | Journaux de passation projet (génération, régénération, accès ws) | ✅ | `knowledge/HandoverJournalPage` | `HandoverJournalController`, `HandoverJournalAccessController` | `handover_journals` | idem |
| **Communication** | Canaux de conversation (types, défauts workspace, membres, archives) | ✅ | `communication/*` | `ConversationController` | `conversations`, `conversation_members` (V20260841) | ConversationController |
| | Messages (envoi, édition, épinglés, fichiers, recherche) | ✅ | ChatWindow, DirectMessages, SharedFiles | `MessageController`, `WorkspaceMessageController` | `messages` | MessageController |
| | Annonces (par ws/département/équipe/projet) | ✅ | AnnouncementsPage + modals | `AnnouncementController` | `announcements` (V20260817) | AnnouncementController |
| **Notifications** | Notifications utilisateur (list/unread/count/read/read-all/dismiss), préférences | ✅ | `productivity/Notifications`, Topbar, `use-notification-socket.ts` | `NotificationController`, `NotificationPreferenceController` | `notifications` (~60 types), `notification_preferences` | NotificationController |
| | Temps réel WebSocket | ✅ | hook WS natif `?token=` | `/ws/notifications` (TextWebSocketHandler) | — | websocket/NotificationWebSocketHandler |
| | Alertes | ✅ | `productivity/Alerts` | `AlertController` | `alerts` (V20260854) | AlertController |
| | Emails transactionnels (Brevo SMTP/API) | ✅ | — | EmailService + Thymeleaf templates | — | commits Brevo, `templates/emails/` |
| **Dashboards & Analytics** | Dashboards par scope (workspace/me/département/projet/équipe) | ✅ | `DashboardPage`, `AdminDashboardPage`, `workspace/WorkspaceAnalyticsPage` | `DashboardController` | requêtes agrégées | DashboardService |
| | Analytics (tâches, activités, documents, notifications, charts, vue admin) | ✅ | `Reporting & Analytics/*` | `AnalyticsController /analytics/**` | agrégations SQL natives | AnalyticsService, builders |
| | Rapports classiques (builder, history, export PDF/CSV) | ✅ | `ReportsPage`, ReportBuilder/History/PDFPreview | Reporting (OpenPDF, POI, Commons-CSV) | `scheduled_reports`, `report_history` (V20260723-24) | pom.xml deps |
| **RH** | Candidats ATS (CRUD, statuts, timeline, notes recruteur, PJ candidat) | ✅ | `departments/hr/CandidatesTab` etc. | `CandidateController`, `RecruiterNoteController`, `CandidateAttachmentController` | `hr_candidates`, `hr_candidate_status_histories`, `hr_recruiter_notes`, `hr_candidate_attachments` (V20260725-28) | controller/hr |
| | Entretiens (planification, participants, feedback, calendrier today/week/upcoming/stats) | ✅ | InterviewsTab, InterviewCalendar | `InterviewController`, `InterviewCalendarController` | `hr_interviews`, `hr_interview_feedback`, `hr_interview_participants` (V20260725-26, V20260850-51) | idem |
| | Employés, documents employés (vérification, expiration), compétences | ✅ | EmployeesTab, SkillsTab, DocumentsTab | `EmployeeController`, `EmployeeDocumentController`, `EmployeeSkillController` | `hr_employees`, `hr_employee_documents`, `hr_employee_skills`, `hr_employee_event_logs` (V20260729-31) | idem |
| | Onboarding (parcours + tâches, stats) | ✅ | OnboardingTab | `OnboardingController` | `hr_onboardings`, `hr_onboarding_tasks` (V20260801) | idem |
| | Présence (check-in/check-out, stats) | ✅ | AttendanceTab | `AttendanceController` | `hr_attendances` (V20260802) | idem |
| | Évaluations de performance (workflow submit/approve/reject/archive) | ✅ | PerformanceReviewsTab | `PerformanceReviewController` | `hr_performance_reviews` (V20260803) | idem |
| | Notifications RH dédiées | ✅ | hr/NotificationsTab | `HrNotificationController` | réutilise `notifications` | idem |
| **Développement** | Sprints (activate/complete/archive, stats) + liaison tâches↔sprint | ✅ | `departments/development/SprintsTab` | `SprintController` | `dev_sprints` (V20260804) | controller/dev |
| **Cybersécurité** | Audits de sécurité (start/complete/archive, stats) + liaison tâches | ✅ | `departments/cybersecurity/*` | `SecurityAuditController` | `security_audits` (V20260806) | controller/cyber |
| **Marketing** | Campagnes (activate/complete/archive, stats) + liaison tâches | ✅ | `departments/marketing/*` | `MarketingCampaignController` | `marketing_campaigns` (V20260807) | controller/marketing |
| | Suivi de modèles ML (ai_models CRUD/status) | ✅ | `departments/ai/ModelsTab` | `AIModelController` | `ai_models` (V20260805, V20260833) | AIModelController |
| **IA générative** | Conversation/chat IA, prompts library, historique IA, analytics IA, rapports IA, Q&A base de connaissances, génération handover | ✅ | `pages/ai/*`, `components/ai/*` (~70 composants) | `ai/` module complet + 5 contrôleurs IA | `ai_history`, `ai_prompts` (V20260824-28) | voir §14 |
| **Profil & préférences** | Profil, sécurité, sessions actives, préférences, préférences de notification | ✅ | `pages/profile/*`, `pages/settings/*` | `UserController PUT /me`, NotificationPreferenceController | `users`, `notification_preferences` | pages/profile |

---

## 4. Actors and Permissions

### 4.1 Rôles globaux (enum `RoleName` — `enums/RoleName.java`)

`SUPER_ADMIN`, `ADMIN`, `MANAGER`, `MEMBER` — hiérarchie Spring confirmée dans `SecurityConfig.roleHierarchy` : `ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_MANAGER > ROLE_MEMBER`.

### 4.2 Rôles de workspace (enum `WorkspaceRole`) et statuts

- `WorkspaceRole` : `OWNER`, `ADMIN`, `MANAGER`, `MEMBER` (table `workspace_members.role`) — distincts des rôles globaux.
- `MemberType` (users) : `EMPLOYEE`, `INTERN`, `PENDING_ACTIVATION`… (`enums/MemberType.java`).
- `UserStatus` : `PENDING_ACTIVATION`, `ACTIVE`, `INACTIVE`, `LOCKED`, `SUSPENDED`, `ARCHIVED`.
- **Aucun rôle « Employee » ou « Intern » en tant que rôle d'autorisation** ; ils existent uniquement comme `MemberType`/statut RH. À ne pas présenter comme rôles RBAC dans le rapport.

### 4.3 Modèle de permissions

- Permissions = lignes de la table `permissions` (code unique, ex. `TASK_CREATE`, `DOCUMENT_UPLOAD`, `HR_*`, `INTERVIEW_*`, `CAMPAIGN_*`, `SPRINT_*`, `SECURITY_AUDIT_*`, `AI_MODEL_*`…) seedées par migrations Flyway (V1, V20260810→V20260818, V20260842, V20260844-45, V20260847-48, V20260852, V20260836).
- Chaîne : `User → UserRole → Role → RolePermission → Permission`. Codes agrégés **dans le JWT** au login puis exposés comme `GrantedAuthority`.

| Acteur | Responsabilité | Actions principales | Restrictions |
|---|---|---|---|
| **SUPER_ADMIN** | Administration globale, bypass | Tout + bypass systématique des vérifications `@workspaceAuth`/`@departmentAuth` | — (bypass documenté dans `WorkspaceAuthorization`) |
| **ADMIN** (global/workspace) | Gestion du workspace et de l'organisation | CRUD users/rôles/départements/équipes/workspaces, dashboards admin, analytics admin (`/admin/activity-overview`, `/admin/project-status`) | Ne bypass pas tout (pas de bypass total comme SUPER_ADMIN) |
| **MANAGER** | Pilotage de son département primaire | Projets/tâches/sprints/campagnes/audits de son département (`primaryDepartment`), HR via `canManageDepartmentHR` limité à son département, dashboards département | Ne voit/gère que son `primaryDepartment` (MANAGER global limité à son dept), pas la gestion workspace |
| **MEMBER** | Exécution | Lecture large (`*_READ`), tâches assignées, commentaires, documents, communication, notifications ; IA interdite en génération | `AIScopeAuthorization.assertCanGenerate` refuse les MEMBER ; pas de gestion organisationnelle |

**Scoping multi-tenant** : quasi toutes les routes sont imbriquées `/api/workspaces/{wsId}/departments/{deptId}/...` et vérifiées par :
- `@permissionEvaluator.hasPermission(auth, 'CODE')` (bean custom `PermissionEvaluator`) ;
- `@workspaceAuth.*` (`WorkspaceAuthorization` : canViewWorkspace, canUpdateWorkspace OWNER/ADMIN, canManageDepartmentHR, canAccessTeam/Project…) ;
- `@departmentAuth.*` (`DepartmentAuthorization`) ;
- garde programmatique `DepartmentScopeGuard` (défense en profondeur dans les services).

---

## 5. Authentication and Security

### 5.1 Flux réel (prouvé par le code)

```text
Utilisateur (LoginPage.tsx, react-hook-form + zod)
   ↓ POST /api/auth/login {email, password}        [route publique permitAll]
Backend AuthController → AuthenticationManager
   ↓ Vérification BCrypt(10) + statut ACTIVE + anti brute-force (5 essais → lock 30 min)
JwtService.generateAccessToken  → claims: sub=email, uid, roles, permissions (codes agrégés), type=ACCESS, jti, iss=collabix
JwtService.generateRefreshToken → claims minimaux (uid, type=REFRESH), persisté en base (refresh_tokens)
   ↓ Réponse LoginResponse
Frontend auth-context.persistAuth() → localStorage["collabix_auth"] = {user, accessToken, refreshToken}
   ↓ Axios request interceptor : header Authorization: Bearer <accessToken>
Routes protégées : JwtAuthenticationFilter (OncePerRequestFilter)
   ↓ Valide signature HS256 + issuer + exp (skew 60s) + type ACCESS + user ACTIVE en base
   ↓ Construit les GrantedAuthority depuis les claims JWT (fallback DB si claim absent)
@PreAuthorize (@permissionEvaluator / @workspaceAuth / @departmentAuth) → 403 sinon
   ↓
401 → intercepteur réponse : refresh single-flight POST /auth/refresh → rejoue la requête
     échec refresh → événement 'session-expired' → SessionExpiredDialog → signOut
```

### 5.2 Détails prouvés

| Élément | Implémentation réelle | Source |
|---|---|---|
| Bibliothèque JWT | JJWT 0.12.7, HS256 (`Keys.hmacShaKeyFor`) | pom.xml, JwtService |
| Durées | access **5 h**, refresh **7 jours** (`app.jwt.*`, JwtProperties) | application.properties |
| Refresh tokens | persistés, révocation unitaire/globale, `findByTokenWithLock` (PESSIMISTIC_WRITE) | RefreshTokenServiceImpl |
| Hashage mots de passe | **BCrypt strength 10** (+ validation robustesse `@Password`/PasswordValidator) | SecurityConfig |
| Sessions | STATELESS, CSRF désactivé (API stateless) | SecurityConfig |
| Routes publiques | preflight OPTIONS, swagger-ui/api-docs, `/api/auth/login|refresh|logout|forgot-password|reset-password|activate|resend-activation`, `/actuator/health` | SecurityConfig |
| Guards backend | `JwtAuthenticationFilter` + `@PreAuthorize` SpEL (3 beans custom) + `DepartmentScopeGuard` + `AIScopeAuthorization` | security/* |
| Guards frontend | `ProtectedRoute` (roles/permissions), `AdminOnly`, `WorkspaceGuard`, `PublicRoute`, `PermissionGuard`/`Can`/`RoleGuard` | pages/auth/, components/layout |
| Stockage token frontend | **localStorage** clé unique `collabix_auth` (JSON user+tokens) — consolidation post-audit sécurité | lib/auth-context.tsx |
| CORS | origines localhost:5173/3000 + `app.cors.allowed-origins` (défaut prod : vercel + railway), allowCredentials, maxAge 3600 | SecurityConfig |
| Headers | CSP stricte configurable, X-Content-Type-Options nosniff, X-Frame-Options DENY, Referrer-Policy SAME_ORIGIN, Permissions-Policy, HSTS 1 an | SecurityConfig |
| Rate limiting | Bucket4j 8.10.1, filtres buckets **auth** (`/api/auth/*` sauf /me), **ai** (URLs contenant /ai/, /analytics/ai…), sinon global → 429 + Retry-After 60 | RateLimitingFilter |
| Anti-XSS | Filtre `XSSFilter` serveur + **DOMPurify** côté front (rendu markdown IA) | chaîne de filtres, package.json |
| WebSocket auth | JWT en query param `?token=`, userId dérivé des claims signés (anti-IDOR), fermeture POLICY_VIOLATION sinon | NotificationWebSocketHandler |
| Verrouillage de compte | 5 tentatives → lock 30 min, unlock admin, automatic-unlock configurable | app.login-security.*, AdminUserController |
| CSRF | Non applicable (API stateless, tokens Bearer) — le mentionner tel quel | SecurityConfig |
| Optimistic locking | `@Version` sur AuditableEntity → 409 en cas de conflit | entity/base/AuditableEntity |

---

## 6. Database

### 6.0 Généralités

- **SGBD : PostgreSQL** (driver `org.postgresql.Driver` ; syntaxe UUID/TIMESTAMPTZ/GIN). Tests : **H2 mémoire MODE=PostgreSQL**, Flyway désactivé, ddl-auto=create-drop.
- **Migrations : Flyway** — **80 fichiers** dans `backend/src/main/resources/db/migration` (V1 init … V20260855). Config : enabled, baseline-on-migrate=true, validate-on-migrate=false. `spring.jpa.hibernate.ddl-auto=validate` → **le schéma est piloté exclusivement par Flyway**.
- Le dossier racine `database/` est **vide** (aucun script SQL tracké).

### 6.1 Liste des tables (~60)

| Table | Description | Clé primaire |
|---|---|---|
| users | Comptes utilisateurs (BCrypt, statut, memberType, dept primaire) | id UUID |
| roles / permissions / role_permissions / user_roles | RBAC | UUID / composite (role_id,permission_id) etc. |
| refresh_tokens | Tokens de rafraîchissement révocables | UUID |
| activation_tokens / password_reset_tokens | Tokens d'activation/reset | UUID |
| workspaces / workspace_members | Tenants + adhésions (role OWNER/ADMIN/MEMBER) | UUID / composite |
| departments / teams / team_members | Organisation interne (teams.manager_id depuis V20260849) | UUID / composite |
| projects | Projets (priority, dates, color, icon, manager) | UUID |
| tasks | Tâches (status, priority, storyPoints, sprint FK, FK audits/campagnes) | UUID |
| checklists / checklist_items | Checklists de tâches | UUID |
| comments / mentions / attachments / activities | Collaboration sur tâches | UUID |
| documents / tags / document_tags / version_history | GED projet (approval workflow) | UUID |
| knowledge_bases | Articles KB (fulltext, ai_processed) | UUID |
| handover_entries / handover_journals / handover_attachments / handover_comments / handover_timeline_events | Passation | UUID |
| conversations / conversation_members / messages | Messagerie | UUID / composite |
| announcements / notifications / notification_preferences / alerts | Communication & alertes | UUID |
| hr_candidates / hr_candidate_status_histories / hr_recruiter_notes / hr_candidate_attachments | ATS recrutement | UUID |
| hr_interviews / hr_interview_feedback / hr_interview_participants | Entretiens | UUID |
| hr_employees / hr_employee_documents / hr_employee_skills / hr_employee_event_logs | Gestion employés | UUID |
| hr_onboardings / hr_onboarding_tasks / hr_attendances / hr_performance_reviews | Cycle de vie RH | UUID |
| dev_sprints / security_audits / marketing_campaigns / ai_models | Modules départements | UUID |
| ai_history / ai_prompts | Historique et templates IA | UUID |
| activities / attachments / user_history / analytics_reports / executive_reports / scheduled_reports / report_history | Support reporting/journalisation | UUID |

### 6.2 Relations principales

| Table A | Relation | Table B | Cardinalité |
|---|---|---|---|
| users ↔ roles | user_roles (PK composite) | roles | N-N |
| roles ↔ permissions | role_permissions | permissions | N-N |
| workspaces ← workspace_members → users | adhésion avec role/status | users | N-N |
| workspaces → departments → teams → projects → tasks | hiérarchie de scoping (FK obligatoires LAZY) | — | 1-N successives |
| tasks ← checklists → checklist_items | 1-N / 1-N | — | composition |
| tasks ← comments ← attachments/mentions | commentaires + PJ | — | 1-N |
| projects → documents / knowledge_bases / handover_journals | contenus projet | — | 1-N |
| users ← refresh_tokens / activation_tokens / password_reset_tokens | cycles d'authentification | — | 1-N |
| conversations ↔ users | conversation_members (joinedAt, lastReadAt, role) | — | N-N |
| hr_departments → hr_candidates → interviews/notes/PJ | ATS imbriqué | — | 1-N |
| tasks → sprints / security_audits / marketing_campaigns | FK optionnelles croisées modules départements | — | N-1 optionnel |

### 6.3 Modèle conceptuel (métier)

Un **workspace** (tenant) appartient à un owner et regroupe des **utilisateurs** (via workspace_members). Il contient des **départements** (RH, Développement, Cybersécurité, Marketing…), chacun avec des **équipes** (membres + manager) et des **projets**. Les projets portent des **tâches** (assignées à un utilisateur, rattachables à un sprint, un audit cyber ou une campagne marketing), enrichies de **checklists, commentaires, pièces jointes, mentions et activités**. Les projets hébergent aussi **documents** et **base de connaissances** (workflow d'approbation, versions, tags). La **passation (handover)** permet de transférer des responsabilités (entrées avec workflow send→accept/reject→complete, journaux générables par IA). La **communication** comprend canaux/messages, annonces et notifications temps réel. Les modules RH couvrent le cycle recrutement→embauche→onboarding→présence→évaluation.

### 6.4 Modèle logique / conventions techniques

- PK **UUID** (`GenerationType.UUID`) pour toutes les entités métier ; tables de jointure en clés composites `@EmbeddedId`.
- Classe de base `BaseEntity` (id) et `AuditableEntity` (createdAt/updatedAt Instant, createdBy/updatedBy UUID via JPA Auditing `CurrentAuditor`, **version @Version** = verrouillage optimiste).
- **Soft delete par enum `status`** (ACTIVE/ARCHIVED/DELETED…) partout ; seul `handover_entries` a un flag booléen `deleted` explicite filtré dans toutes ses requêtes.
- Enums stockés `STRING` (TaskStatus, TaskPriority, WorkspaceStatus, CandidateStatus, InterviewStatus, SprintStatus, CampaignStatus, AuditStatus, ModelStatus, ApprovalStatus, DocumentStatus, NotificationType ~60 valeurs…).
- Index : nombreux index composites métier + **index full-text PostgreSQL (GIN)** sur titres/documents (V20260823).
- Contraintes d'unicité notables : `(owner_id,name)` workspaces, `(department_id,name)` projets/équipes, `(project_id,title)` tâches, email utilisateur unique.
- Pas de triggers ni procédures stockées ; toute la logique est applicative (services Spring).

---

## 7. Entities and Classes

### 7.1 Classes de base

| Classe | Package | Rôle |
|---|---|---|
| `BaseEntity` | entity.base | @MappedSuperclass, id UUID auto-généré |
| `AuditableEntity` | entity.base | hérite BaseEntity + createdAt/updatedAt (@CreatedDate/@LastModifiedDate), createdBy/updatedBy (@CreatedBy/@LastModifiedBy), version @Version |
| IDs composites (`UserRoleId`, `RolePermissionId`, `TeamMemberId`, `WorkspaceMemberId`, `ConversationMemberId`) | entity.ids | @Embeddable Serializable pour tables de jointure |

### 7.2 Entités métier principales

| Classe | Responsabilité | Attributs principaux | Relations |
|---|---|---|---|
| `User` (table users) | Compte utilisateur | firstName, lastName, email(unique), password(BCrypt), memberType, enabled, status(UserStatus), profilePicture, lastLoginAt, failedLoginAttempts, lockedAt, archivedAt, primaryDepartment | userRoles, refreshTokens(cascade ALL), workspaceMembers, teamMembers ; ManyToOne Department |
| `Role` / `Permission` | RBAC | Role.name(RoleName unique), description ; Permission.code(unique), displayName, description | Role→RolePermission→Permission |
| `Workspace` | Tenant | name, description, status(WorkspaceStatus) | owner(User obligatoire), workspaceMembers |
| `Department` | Département | workspace(obligatoire), name, description, status | projets, équipes via FK |
| `Team` / `TeamMember` | Équipe | name, description, status, manager(User, V20260849) ; TeamMember: status, timestamps manuels | workspace+department obligatoires ; members composite (team,user) |
| `Project` | Projet | department(obligatoire), manager(optionnel), name, description(2000), status, priority(ProjectPriority), startDate/endDate(LocalDate), color(7), icon | tasks, documents, knowledgeBases, handoverJournals |
| `Task` | Tâche | project(obligatoire), assignee(optionnel), sprint/securityAudit/marketingCampaign(optionnels), title, description, status(TaskStatus), priority(TaskPriority), dueAt/startDate(Instant), storyPoints | checklists, comments, attachments, activities |
| `Checklist`/`ChecklistItem` | Checklist | task(obligatoire), title; item: content(500), completed, sortOrder | composition |
| `Comment` | Commentaire | task, content(TEXT 100k), status(CommentStatus=soft delete), parentCommentId(UUID simple, threads non implémentés) | mentions, attachments |
| `Document` | Document GED | project(obligatoire), task(optionnel), fileName, mimeType, fileSize, category, tags CSV, viewCount, storagePath, storageType("LOCAL"), documentVersion, aiProcessed, approvalStatus(ApprovalStatus), approvedBy/At, status(DocumentStatus) | versions (version_history), tags N-N |
| `KnowledgeBase` | Article KB | contenu, catégorie, ai_processed/embeddings (RAG), approbation, versions | projet obligatoire |
| `HandoverEntry` | Passation | workflow (send/submit/accept/reject/complete/archive), **deleted Boolean** (seul soft delete par flag) | attachments/comments/timeline_events |
| `Notification` | Notification | recipient+workspace (obligatoires), ~60 notificationType, title/body/linkUrl/priority/category/groupKey, readAt, status(NotificationStatus), resourceType/resourceId génériques, FK optionnelles project/task/comment/document/knowledgeBase/handoverEntry | ~11 index |
| `Conversation`/`Message` | Messagerie | type(ConversationType), membres avec lastReadAt ; messages épinglables/fichiers | conversation_members composite |
| `Candidate` (+status histories, recruiter notes, attachments) | Candidat ATS | statut CandidateStatus, source, timeline | département RH |
| `Interview` (+feedback, participants) | Entretien | InterviewStatus/Type, planning ; feedback ; participants ( BaseEntity sans audit) | candidate |
| `Employee` (+documents, skills, event logs) | Employé | EmploymentStatus, ContractType… | department RH |
| `Attendance` / `Onboarding(+tasks)` / `PerformanceReview` | Cycle de vie RH | AttendanceStatus, OnboardingStatus/TaskStatus, ReviewStatus, PerformanceLevel | employee |
| `Sprint` / `SecurityAudit` / `MarketingCampaign` / `AIModel` | Modules départements | SprintStatus ; AuditStatus/Type/Priority ; CampaignStatus/Type/Priority ; ModelType/Status, accuracy | tasks liées (FK optionnelle depuis Task) |
| `ai.entity.AIPrompt` / `ai.entity.AIHistory` | IA | code unique, catégorie AIPromptCategory, promptTemplate TEXT, active ; AIHistory : userId/workspaceId/departmentId UUID "plats" (sans FK JPA), provider(AIProvider), model, prompt/response TEXT, executionTime, tokenCount, success | — |

### 7.3 Typologie des classes backend (~76 repositories, ~55 contrôleurs)

- **Entity** : `entity/*` + `ai/entity/*` (~60 entités).
- **DTO** : `dto/*` organisés par module (auth, user, role, permission, workspace, organisation/{project,task,checklist,comment,handover,mention,…}, hr, dev, cyber, marketing, communication, notification, alert, Dashboard/scope/widget, Document, Knowledgebase, announcement, ai) + `ai/dto/{request,response}` + `reporting/analytics/dto/{metrics,chart,admin}`.
- **Mapper** : MapStruct 1.6.3 (`mapper/*`, `ai/mapper/*`, génération à la compilation).
- **Repository** : Spring Data JPA, 73 interfaces + 16 classes `Specification` (filtrage dynamique : UserSpecification, EmployeeSpecification, CandidateSpecification, SprintSpecification, MarketingCampaignSpecification, AIModelSpecification…).
- **Service** : interfaces + impl (`service/impl`, `service/hr`, `service/dev`, `service/cyber`, `service/marketing`, `service/ai`).
- **Controller** : 54 contrôleurs REST + 3 dans `ai/controller`.
- **Security** : jwt (JwtService, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtProperties), permission (PermissionEvaluator), workspace/department (beans SpEL + guards), audit (CurrentAuditor), ai (AIScopeAuthorization).
- **Configuration** : SecurityConfig, WebSocketConfig, CorsConfigurationSource, JpaAuditingConfig, RestClientConfig (IA), AsyncConfig, CacheConfig (Caffeine), RateLimitingFilter, XSSFilter.
- **Exception** : GlobalExceptionHandler + exceptions métier typées.
- **Event** : NotificationCreatedEvent, AccountActivationEmailRequestedEvent + listeners async.
- **Reporting** : reporting/analytics/{builder,dto} — TaskMetricsBuilder, ActivityMetricsBuilder, ChartDataBuilder, collectors (Analytics/Reporting/Handover/Knowledge).

---

## 8. Backend Architecture

**Architecture en couches monolithique confirmée** :

```text
Frontend SPA (React)
   ↓ HTTPS REST /api/** (axios) + WebSocket /ws/notifications
Filtres servlet : XSSFilter → RateLimitingFilter(Bucket4j) → JwtAuthenticationFilter
   ↓
Controller REST (@RestController, validation @Valid, ApiResponse<T> enveloppe standard)
   ↓ autorisation : @PreAuthorize(@permissionEvaluator / @workspaceAuth / @departmentAuth)
Service (interface + Impl, logique métier, transactions, guards programmatiques)
   ↓
Repository (Spring Data JPA + Specifications + JPQL/SQL natif)
   ↓ JPA/Hibernate (ddl-auto=validate)
PostgreSQL (schéma piloté par Flyway)
```

- **Pattern Repository + Service layer + DTO + MapStruct** : oui, systématique.
- **Injection de dépendances** : constructeur via Lombok `@RequiredArgsConstructor`.
- **Monolithe modulaire** : packages par domaine (controller/hr, controller/dev…) et module IA quasi autonome (`com.trio.backend.ai.*`) mais déployé dans le même jar. Pas de microservices.
- **Pagination Spring** (`Page<T>`) sur les listes ; enveloppe uniforme `ApiResponse<T>{success,data,message}`.
- **Caching** : Caffeine (`@Cacheable("permissions")` sur PermissionServiceImpl).
- **Async** : listeners d'événements `@Async @EventListener` (emails, push WS découplés des transactions).

---

## 9. Frontend Architecture

### 9.1 Stack et structure

React 18.3 + TypeScript 5.5 strict + Vite 5.4 + Tailwind CSS 3.4 (dark mode `class`, tokens CSS variables). Arbre providers : `BrowserRouter > ThemeProvider > QueryClientProvider > AuthProvider > ToastProvider > GlobalAuthHandler > ErrorBoundary`.

### 9.2 Routing et guards (App.tsx)

- React Router DOM 7 (`createBrowserRouter` style déclaratif dans App.tsx), lazy loading Suspense de toutes les pages, chunking vendor manuel dans vite.config.ts.
- Guards réels : `ProtectedRoute` (requiredRoles/requiredPermissions), `PublicRoute`, `AdminOnly`, `WorkspaceGuard` (exige `?ws=` pour routes workspace-dépendantes), redirections par rôle sur `/app/dashboard` (Admin → AdminDashboardPage ; Manager avec dept → page département ; sinon DashboardPage membre). Erreurs dédiées : `/401`, `/403`, `/404`, `/session-expired`.

### 9.3 État et données

- **TanStack Query 5** massivement utilisé : query-client (staleTime 30s, gcTime 10 min, retry 2, refetchOnWindowFocus false), clés par domaine (query-keys.ts factory + clés locales par hooks), invalidation ciblée après mutations, **updates optimistes** (useUpdateTask), agrégations `Promise.allSettled`.
- **Axios 1.18** : instance unique (`lib/api.ts`) — baseURL `VITE_API_BASE_URL` (dev : proxy `/api`→localhost:8081 ; prod fallback Railway), timeout `VITE_API_TIMEOUT`, intercepteur Bearer, unwrap `ApiResponse<T>`, normalisation pages Spring, normalisation erreurs (`error.normalized{message,status,fieldErrors}`), **refresh 401 single-flight** partagé (promise) + bus d'événements auth (`auth-events.ts`).
- **AuthContext** : localStorage `collabix_auth`, décodage JWT maison (claim permissions), sync multi-onglets (event `storage`), clear du cache Query au changement d'utilisateur.

### 9.4 Formulaires, UI, temps réel

- **react-hook-form 7 + zod 3 + @hookform/resolvers** (LoginPage, CreateUserModal, ActivationForm, ResetForm, formulaires HR).
- Design system interne : 29 composants `components/ui/*` (Button, Modal, Table, Tabs, Toast, Charts SVG maison — pas de recharts…), skeletons, ErrorBoundary/NotFound/NetworkError.
- Temps réel : hook natif WebSocket `use-notification-socket.ts` → `ws(s)://host/ws/notifications?token=` ; invalide les clés query notifications à chaque message.
- Thème : light/dark/system persisté (`collabix-theme`).

---

## 10. REST API

**57 contrôleurs**, ~350 endpoints sous `/api/**`. Convention transverse : create / liste-recherche(paginée) / détail / update / transitions (activate-complete-archive-restore) / delete / stats. Réponses `ApiResponse<T>`.

### 10.1 Endpoints par module (résumé exhaustif des chemins de base)

| Module | Contrôleur(s) | Base | Exemples d'endpoints clés |
|---|---|---|---|
| Auth | AuthController, ActivationController | `/api/auth` | POST login/refresh/logout/forgot-password/reset-password/change-password ; GET/POST activate ; POST resend-activation ; GET me (authentifié) |
| Admin users | AdminUserController | `/api/admin/users` | POST /{userId}/unlock |
| Users | UserController, UserHistoryController | `/api/workspaces/{wsId}/users` | CRUD + search paginé + PUT roles + activate/deactivate/suspend/reactivate/archive/restore + DELETE soft & `/permanent` + statistics ; history paginé |
| Rôles/Permissions | RoleController, PermissionController | `/api/roles`, `/api/permissions` | GET liste/détail (lecture) |
| Workspaces | WorkspaceController | `/api/workspaces` | CRUD, archive/restore, /archived, teams |
| Departments | DepartmentController | `.../departments` | CRUD + details enrichi + restore + soft/permanent delete |
| Teams | TeamController, TeamMemberController | `.../departments/{dId}/teams[/{tId}/members]` | CRUD équipes + gestion membres |
| Projects | ProjectController, WorkspaceProjectController | `.../projects` | CRUD, /archived, restore, vue transverse ws |
| Tasks | TaskController | `.../projects/{pId}/tasks` | CRUD paginé filtré, /archived, restore |
| Checklists | ChecklistController | `.../tasks/{tId}/checklists` | CRUD + items + toggle |
| Comments/Mentions/Activity/Attachments | CommentController, MentionController, ActivityController, AttachmentController | `.../tasks/{tId}/comments|mentions|activities|attachments` | CRUD complets, upload/remplacement PJ (tâche & commentaire) |
| Documents | DocumentController, WorkspaceDocumentController | `.../projects/{pId}/documents` | upload multipart, download, search, submit-for-approval/approve/reject, archive/restore, versions |
| Knowledge base | KnowledgeBaseController | `.../knowledge-base` | CRUD, catégories, approbation, versions |
| Handover | HandoverEntryController, HandoverJournalController, HandoverJournalAccessController | `/handovers`, `.../handover-logs` | inbox/sent/my-entries, send/submit/accept/reject/complete/archive, commentaires/PJ/timeline ; journaux generate/regenerate |
| Dashboards | DashboardController | `/api/workspaces/{wsId}/dashboard/**` | workspace/me/département/projet/équipe |
| Analytics | AnalyticsController | `.../analytics` | tasks/activities/documents/notifications/charts + admin/activity-overview + admin/project-status |
| Communication | ConversationController, MessageController, WorkspaceMessageController, AnnouncementController | `.../conversations|messages|announcements` | canaux (types, défauts, DM, membres, unread-count), messages (pinned/files/search), annonces multi-cibles |
| Notifications | NotificationController, NotificationPreferenceController, AlertController | `.../notifications|-preferences|alerts` | list/unread/count/read/read-all/dismiss ; préférences CRUD ; alertes read/count |
| RH | CandidateController, RecruiterNoteController, CandidateAttachmentController, InterviewController, InterviewCalendarController, OnboardingController, EmployeeController, EmployeeDocumentController, EmployeeSkillController, AttendanceController, PerformanceReviewController, HrNotificationController | `/api/workspaces/{wsId}/departments/{dId}/...` | ATS complet (status/timeline/stats/notes/PJ+download), entretiens (participants, feedback, calendrier today/week/upcoming/completed/stats), onboarding+tasks, employés+docs(vérif/expiring)+skills(certifs expirantes), attendance check-in/out, reviews workflow submit/approve/reject/archive, notifications RH |
| Dev | SprintController | `.../sprints` | CRUD, activate/complete/archive, stats |
| Cyber | SecurityAuditController | `.../audits` | CRUD, start/complete/archive, stats |
| Marketing | MarketingCampaignController | `.../campaigns` | CRUD, activate/complete/archive, stats |
| Modèles ML | AIModelController | `.../models` | CRUD, status, archive, stats |
| IA | AnalyticsAIController `/api/analytics/ai`, ReportingAIController `/api/reports/ai`, KnowledgeAIController `/api/knowledge/ai`, HandoverAIController `/api/handover/ai`, AIPromptController `/api/ai/prompts`, AIHistoryController `/api/ai/history`, AITestController `/api/ai/test` | — | generate/regenerate/edit/approve/reject + history ; knowledge ask/search ; prompts actifs ; tests providers |

### 10.2 Conventions transverses (prouvées)

- Codes HTTP gérés par `GlobalExceptionHandler` (voir §17). Validation `@Valid` → 400 avec `ApiError[field,message]`.
- Pagination : paramètres Spring `?page=&size=&sort=` → réponse normalisée côté front.
- Filtres/recherche : endpoints `/search` + Specifications dynamiques (users, documents, messages, candidats, employees, sprints, campagnes, audits…).
- Upload/download : multipart max 10 MB (`MAX_FILE_SIZE_MB`), stockage local `app.storage.local.path` (`./uploads`).

---

## 11. Use Cases

Cas d'utilisation reconstruits **uniquement à partir du code** (acteurs = rôles RBAC réels). Les 5 plus structurants pour le rapport :

### UC-1 : S'authentifier et accéder à son espace
- **Acteur** : tout utilisateur (PENDING → ACTIVE).
- **Préconditions** : compte existant, statut ACTIVE.
- **Nominal** : LoginPage (zod) → POST /api/auth/login → vérif BCrypt + statut + anti brute-force → tokens JWT → localStorage → redirection selon rôle (`/app/dashboard` : admin→AdminDashboard, manager→département, membre→DashboardPage) → ProtectedRoute/WorkspaceGuard chargent le workspace `?ws=`.
- **Alternatifs** : compte PENDING → lien activation email (ActivationController + pages/activate) ; mot de passe oublié → reset par token ; 5 échecs → LOCKED 30 min → déverrouillage admin ; token expiré → refresh single-flight → sinon SessionExpiredDialog.
- **Endpoints** : `/api/auth/*` ; **Frontend** : pages/auth/*, lib/auth-context.tsx.

### UC-2 : Gérer des projets et tâches (workflow Kanban)
- **Acteurs** : MANAGER (création), MEMBER (exécution assignée).
- **Nominal** : créer projet (PROJECT_CREATE, scoping département) → créer tâches (TASK_CREATE, assignee, priorité, storyPoints, dueAt) → transitions de statut (couvert par `TaskKanbanWorkflowIntegrationTest`) → checklists/commentaires/PJ/mentions → activités journalisées → dashboards et analytics mis à jour.
- **Exceptions** : 403 hors département primaire (DepartmentScopeGuard) ; 409 nom de projet dupliqué dans le département (contrainte unique gérée par GlobalExceptionHandler) ; 409 verrou optimiste.
- **Endpoints** : ProjectController, TaskController, ChecklistController, CommentController, AttachmentController, ActivityController.

### UC-3 : Recruter un candidat (module RH)
- **Acteur** : MANAGER RH / ADMIN (permissions HR_*), lecture possible pour WS:view + *_READ.
- **Nominal** : créer candidat → changement de statuts avec historique (timeline) → notes recruteur → planifier entretien (+participants, feedback) → calendrier entretiens (today/week/upcoming/stats) → embauche → employé + onboarding (parcours+tâches) → présence (check-in/out) → évaluations de performance (submit/approve/reject).
- **Exceptions** : `canManageDepartmentHR` limite le manager global à son `primaryDepartment`.
- **Endpoints** : controller/hr/* (12 contrôleurs) ; **Frontend** : departments/hr/*.

### UC-4 : Passation de responsabilités (Handover)
- **Acteurs** : cédant (HANDOVER_CREATE/UPDATE) et repreneur (accept/reject).
- **Nominal** : créer entrée (contenu, PJ) → send/submit → destinataire accepte ou rejette (commentaires possibles) → complete/archive ; timeline automatique ; journal de passation projet générable manuellement (`POST .../handover-logs/generate`) ou par IA (`/api/handover/ai/generate`) avec approbation.
- **Endpoints** : HandoverEntryController, HandoverJournalController, HandoverAIController ; **Frontend** : pages/handover/*.

### UC-5 : Générer une analyse/rapport IA
- **Acteur** : MANAGER+ (MEMBER refusé par AIScopeAuthorization), permission ANALYTICS_VIEW / REPORT_CREATE / HANDOVER_CREATE / KNOWLEDGE_BASE_READ.
- **Nominal** : choix du scope (WORKSPACE/DEPARTMENT/PROJECT/TEAM) → collecte de données réelles (AnalyticsDataCollector/ReportingDataCollector…) → PromptBuilder construit le prompt (templates seedés ai_prompts) → PipelineExecutor exécute Gemini puis fallback Groq → réponse persistée (analytics_reports / executive_reports / handover_journals + ai_history) → approbation workflow (approve/reject) → consultation ReportViewer.
- **Exceptions** : 403 scope interdit ; 502/503 provider IA ; 429 rate limit IA ; tous providers en échec → AIProviderException.
- Voir §14 pour le détail technique.

Autres cas d'utilisation secondaires documentables : gestion documents avec approbation, messagerie canaux/DM, annonces, notifications temps réel, administration users/rôles/permissions, analytics & rapports exportables.

---

## 12. UML Data

### 12.1 Diagramme de cas d'utilisation — données
Acteurs : Super Admin, Admin, Manager, Member (généralisation Manager→Member→… via roleHierarchy) ; cas d'utilisation listés §11 ; relations d'inclusion notables : « Générer rapport IA » « include » « Collecter données analytics » ; « Toute fonction » « include » « S'authentifier ».

### 12.2 Diagramme de classes — données
Cœur : User —< UserRole >— Role —< RolePermission >— Permission ; Workspace —< Department —< Team ; Department —< Project —< Task ; Task —< Checklist —< ChecklistItem ; Task —< Comment —< Mention/Attachment ; Project —< Document/KnowledgeBase ; Workspace —< Notification ; héritage : toutes les entités métier héritent de AuditableEntity (BaseEntity). Détails attributs §6/§7.

### 12.3 Diagrammes de séquence recommandés (flux prouvés)

1. **Login + JWT** : Actor → LoginPage → AuthController → LoginSecurityService → JwtService → RefreshTokenService(DB) → Frontend localStorage → requêtes Bearer.
2. **Refresh token sur 401** : axios interceptor → POST /auth/refresh → RefreshTokenServiceImpl (lock pessimiste) → rejoue requête.
3. **Création tâche** : TasksPage (mutation TanStack) → TaskController → @PreAuthorize(workspaceAuth+TASK_CREATE) → TaskServiceImpl → TaskRepository → PostgreSQL → invalidation query.
4. **Upload document** : DocumentsPage (FormData) → DocumentController.upload → StorageService (./uploads) → DocumentRepository → notification créée → NotificationCreatedEvent → WS push + email Brevo.
5. **Notification temps réel** : service métier → NotificationService.save → event async → NotificationWebSocketHandler.sendNotification(recipientId) → front use-notification-socket → invalidate queries.
6. **Génération IA analytics** : AnalyticsAIController.generate → AIScopeAuthorization.assertCanGenerate → AnalyticsDataCollector → PromptBuilder → PipelineExecutor → GeminiService (RestClient) [fallback GroqService] → AIHistoryService.persist → AnalyticsReportRepository.save → response DTO.

### 12.4 Diagrammes d'activité pertinents
- Cycle de vie utilisateur : PENDING_ACTIVATION → ACTIVE → INACTIVE/SUSPENDED/LOCKED → ARCHIVED (+ restore / permanent delete).
- Workflow document : draft → submit-for-approval → approve/reject → archive/restore (versions).
- Workflow handover : create → send/submit → accept/reject → complete/archive.
- Workflow performance review : create → submit → approve/reject → archive.

---

## 13. Technical Architecture

```text
Utilisateur (navigateur)
    ↓ HTTPS
Frontend SPA React — Vercel (collabix-chi.vercel.app, rewrite SPA)
    ↓ REST /api/** (VITE_API_BASE_URL) + WebSocket /ws/notifications?token=
Backend Spring Boot (Docker, eclipse-temurin:21-jre) — Railway (collabix-production-eead.up.railway.app)
    ├── Spring Security (JWT HS256, Bucket4j rate limiting, CORS/CSP)
    ├── Services métier (55 controllers / ~350 endpoints)
    ├── Flyway migrations (80 scripts SQL)
    ├── Emails : Brevo (SMTPS relay 465 ou API REST si BREVO_API_KEY)
    └── IA : Google Gemini API (gemini-2.5-flash) + Groq API (openai/gpt-oss-120b)
    ↓ JDBC
PostgreSQL managé (Railway) — schéma Flyway, UUID, full-text GIN
Stockage fichiers local conteneur ./uploads (max 10 Mo)
```

Composants réellement présents uniquement : pas de Redis, pas de message broker (événements Spring internes), pas de S3 (storageType "LOCAL" codé en dur par défaut).

---

## 14. AI Features

Module IA réel et complet — packages `com.trio.backend.ai.*` + `service/ai`, `controller/ai`, front `pages/ai`, `components/ai`.

| Élément | Réalité prouvée | Source |
|---|---|---|
| Fournisseurs | **Google Gemini** (modèle `gemini-2.5-flash`) et **Groq** (API compatible OpenAI, modèle `openai/gpt-oss-120b`). Aucune clé OpenAI directe. | application.properties (`ai.gemini.*`, `ai.groq.*`), GeminiServiceImpl/GroqServiceImpl |
| Appels HTTP | RestClient Spring (config `RestClientConfig`), clés via variables `GEMINI_API_KEY` / `GROQ_API_KEY` (jamais en dur) | ai/configuration |
| Orchestration | `PipelineExecutor` : chaînes par tâche (enum AITask). GENERAL_CHAT → GROQ seul ; 13 tâches (ANALYTICS_*, HANDOVER_*, KNOWLEDGE_SEARCH, DOCUMENT_*, REPORT_*) → **GEMINI puis fallback GROQ** ; sortie d'un provider = entrée enrichie du suivant ; échec total → AIProviderException | PipelineExecutor.java |
| Prompts | Templates seedés en base (`ai_prompts`, catégorie AIPromptCategory : handover, analytics, knowledge, reporting — V20260825/27/28) + `PromptBuilder` injectant contexte collecté | migrations, PromptBuilder |
| Données envoyées | Agrégats réels du workspace/département/projet/équipe via collectors (AnalyticsDataCollector, ReportingDataCollector incluant KPI/tendances/risques + derniers journaux handover, HandoverDataCollector, KnowledgeDataCollector avec recherche plein texte/embeddings RAG) | reporting.analytics |
| Cas d'utilisation IA | Conversation/chat, bibliothèque de prompts (run modal), historique IA filtrable, analyse analytics, génération de rapports (avec édition/approbation), Q&A base de connaissances (ask/search sources), génération journaux de passation, suivi de modèles ML (ai_models CRUD) | contrôleurs IA + pages/ai/* |
| Historique & traçabilité | Chaque appel provider persisté dans `ai_history` (provider, model, prompt, response, executionTime, tokenCount, success, user/workspace/department) | AIHistoryServiceImpl |
| Sécurité IA | Rate limiting dédié bucket "ai" ; `AIScopeAuthorization` (MEMBER interdit, MANAGER limité à son périmètre, validation d'existence des entités de scope) ; endpoints sous permissions (ANALYTICS_VIEW, REPORT_CREATE, KNOWLEDGE_BASE_READ, HANDOVER_CREATE, AI_MODEL_READ/CREATE) | security/ai, RateLimitingFilter |
| Erreurs | AIConfigurationException(500), AIConnectionException(503), AIProviderException(502), AIResponseException/AIException(500) | exception.GlobalExceptionHandler |
| Limitations connues | Prompt injection **non protégé** (documenté dans docs/SECURITY_REPORT.md comme vulnérabilité restante) ; sanitization erreurs IA incomplète | docs/SECURITY_REPORT.md |

Workflow réel :
```text
Utilisateur (page AI, choix scope) 
 ↓ Frontend services/reporting-ai-service.ts (axios)
Controller IA (@PreAuthorize) → AIScopeAuthorization (403 si interdit)
 ↓ *DataCollector (données DB agrégées)
PromptBuilder (template ai_prompts + contexte) → PipelineExecutor
 ↓ Gemini API (gemini-2.5-flash) — fallback Groq API
AIHistory persist → Rapport sauvegardé (analytics_reports/executive_reports/handover_journals)
 ↓ ApiResponse
Frontend (ReportViewerPage, ConversationChatView…) + historique consultable
```

---

## 15. Notifications and Automation

| Mécanisme | Réalité | Flux |
|---|---|---|
| Notifications in-app | Entité `Notification` (~60 types : MENTION, DOCUMENT_UPLOADED, CANDIDATE_*, SPRINT_*, CAMPAIGN_*, NEW_MESSAGE…), statuts UNREAD/READ/DISMISSED/ARCHIVED | Service → save → event |
| Temps réel WebSocket | Endpoint brut `/ws/notifications` (pas STOMP), sessions multiples par user, auth JWT `?token=` | NotificationCreatedEvent → listener @Async → handler.sendNotification(recipientId, JSON) → front invalide les queries TanStack |
| Emails transactionnels | Thymeleaf templates (activation, reset, notifications, HR) ; envoi via **Brevo SMTP relay** (SMTPS 465) ou **Brevo REST API** si BREVO_API_KEY ; échecs non bloquants (log) | AccountActivationEmailRequestedEvent / NotificationCreatedEvent → EmailService |
| Préférences de notification | Table `notification_preferences` + UI dédiée | NotificationPreferenceController |
| Alertes | Module `alerts` séparé (read/count/delete) | AlertController |
| Jobs/scheduler | **Aucun @Scheduled trouvé** — purge/expirations non automatisées (`NON TROUVÉ DANS LE CODE`) | grep sans résultat |
| Webhooks/workflows externes | `NON TROUVÉ DANS LE CODE` | — |

---

## 16. Testing

### 16.1 Backend — 24 classes de test (réel)

**Unitaires (Mockito)** : TeamServiceImplTest, TeamMemberServiceImplTest, TaskServiceImplTest, ProjectServiceImplTest, HandoverSupportTest, HandoverJournalServiceImplTest, HandoverEntryServiceImplTest, EmailServiceImplTest, DepartmentServiceImplTest ; security : JwtServiceTest, DepartmentScopeGuardTest, DepartmentAuthorizationTest, AIScopeAuthorizationTest.

**Intégration (@SpringBootTest + H2 MODE=PostgreSQL)** : TaskKanbanWorkflowIntegrationTest, TaskAssignmentIsolationIntegrationTest, ProjectDepartmentIsolationIntegrationTest, DocumentModuleE2EIntegrationTest, AlertModuleIntegrationTest, AIScopeIntegrationTest (+ fixtures/config support).

| Test | Type | Fonctionnalité testée | Résultat | Fichier |
|---|---|---|---|---|
| DocumentModuleE2EIntegrationTest | Intégration E2E | Documents upload→download→auth | 10/10 PASS (17/08/2026) | integration/projects |
| AlertModuleIntegrationTest | Intégration | Module alertes | 4/4 PASS (17/08) | integration/alert |
| ProjectDepartmentIsolationIntegrationTest | Intégration | Isolation multi-départements projets | 12/12 PASS | integration/projects |
| AIScopeIntegrationTest | Intégration | Périmètres IA | 6/6 PASS | integration/ai |
| TaskAssignmentIsolationIntegrationTest | Intégration | Isolation tâches assignées | 4/4 PASS | integration/projects |
| TaskKanbanWorkflowIntegrationTest | Intégration | Workflow Kanban tâches | 6/6 PASS | integration/projects |
| AIScopeAuthorizationTest | Unité | Autorisation IA | 4/4 PASS | security/ai |
| HandoverJournalServiceImplTest | Unité | Journaux passation | 18/18 PASS | service |
| TaskServiceImplTest | Unité | Service tâches | 8/8 PASS | service |

- **Total exécuté : 72 tests, 0 échec, 0 erreur, 0 skip** — mais rapports Surefire **partiels** (13 et 17/08/2026) ; les autres classes n'ont pas de rapport récent → suite complète non relancée (`À CONFIRMER`).
- **Pas de JaCoCo / mesure de couverture** (`NON TROUVÉ DANS LE CODE`). Pas de config Surefire/Failsafe custom.
- Config test : `backend/src/test/resources/application.properties` — H2 mem, ddl-auto=create-drop, Flyway off.

### 16.2 Frontend

**AUCUN TEST AUTOMATISÉ** — aucun fichier *.test.*/*.spec.*, ni Vitest/Jest dans package.json. Qualité assurée uniquement par **ESLint 9** + **`tsc --noEmit`** (scripts lint/typecheck). À indiquer honnêtement dans le rapport.

---

## 17. Error Handling

### Backend (`exception.GlobalExceptionHandler`, @RestControllerAdvice, format uniforme `ApiResponse.failure`)

| Exception | HTTP |
|---|---|
| ResourceNotFoundException | 404 |
| BadRequestException / ValidationException / MethodArgumentNotValidException (liste champs) / MethodArgumentTypeMismatchException | 400 |
| ConflictException / ObjectOptimisticLockingFailureException / DataIntegrityViolationException (projet dupliqué → 409, sinon 500) | 409 |
| UnauthorizedException / BadCredentialsException (« Invalid email or password ») / AuthenticationException | 401 |
| ForbiddenException / AccessDeniedException | 403 |
| AIProviderException / AIConnectionException / AIConfigurationException / AIResponseException | 502 / 503 / 500 / 500 |
| ReportException | 400 |
| NoResourceFoundException | 404 |
| Catch-all Exception | 500 « An unexpected error occurred » (prod : détails masqués via application-prod.properties) |

JwtAuthenticationEntryPoint → 401 JSON `{"success":false,"message":"Authentication required."}`. Rate limiter → 429 + Retry-After: 60.

### Frontend
- Normalisation d'erreurs axios (`error.normalized{message,status,fieldErrors}`).
- États loading/error par page : skeletons, PageLoader, EmptyState, NetworkErrorPage, ErrorBoundary global, NotFoundPage, UnauthorizedPage(401), ForbiddenPage(403), SessionExpiredDialog/Page.
- Toasts de succès/erreur (ToastProvider), formulaires avec messages de validation zod affichés.

## 18. Performance

Mécanismes **réellement présents** :
- Pagination serveur systématique (Page<T>, endpoints /search) + composant Pagination UI.
- TanStack Query : cache client (staleTime 30s), déduplication, invalidations ciblées, updates optimistes, lazy loading de toutes les routes (React.lazy/Suspense) + chunking vendor Vite.
- DB : index composites métier, index full-text GIN PostgreSQL, @BatchSize(20) sur collections LAZY, fetch LAZY par défaut, open-in-view=false, agrégations SQL natives (GROUP BY jour…).
- Cache applicatif Caffeine (permissions).
- Verrouillage optimiste (@Version) plutôt que pessimiste généralisé.

Non présents : compression serveur configurée (`NON TROUVÉ DANS LE CODE`), CDN fichiers statiques backend, cache HTTP.

## 19. Security

Récapitulatif des mécanismes prouvés (détail §5) : JWT HS256 signé (secret ≥32 octets, warn sinon) + distinction type ACCESS/REFRESH anti-usurpation croisée ; refresh persistés révocables + rotation ; BCrypt(10) ; verrouillage brute-force ; RBAC permissions fines en base + embarquées dans le JWT ; scoping workspace/département multi-niveaux + guards programmatiques (anti-IDOR, y compris WebSocket) ; rate limiting Bucket4j 3 buckets ; XSSFilter + DOMPurify ; CSP/HSTS/X-Frame-Options/nosniff/Referrer-Policy/Permissions-Policy ; CORS restrictive allowCredentials ; secrets exclus du code (variables d'environnement, .env ignoré par git) ; validation @Valid + PasswordValidator ; JPA paramétré (protection SQL injection par requêtes préparées) ; magic bytes vérifiés à l'upload (correctif documenté) ; actuator restreint en prod (health,info) ; Swagger désactivé en prod.

Résidus documentés (à mentionner comme perspectives) : prompt injection IA non protégée, audit logging partiel, blacklist tokens au logout incomplète — source docs/SECURITY_REPORT.md (score 84/100).

## 20. Deployment

| Composant | Réalité | Preuve |
|---|---|---|
| Backend Dockerfile | Multi-stage : build `maven:3.9.6-eclipse-temurin-21` (`mvn clean package -DskipTests`) → runtime `eclipse-temurin:21-jre`, EXPOSE 8080, `-Djava.net.preferIPv4Stack=true` (fix SMTP Docker) | backend/Dockerfile |
| Hébergement backend | **Railway** (DATABASE_URL Railway convertie en JDBC, CORS fallback railway, commits "railway fixed") ; commit historique "Dockerfile pour Render" → Render évoqué mais aucune config render.yaml (`À CONFIRMER`) | application.properties, git log |
| Frontend | **Vercel** : vercel.json rewrite SPA, projets liés (.vercel/project.json racine et frontend/project) | fichiers .vercel |
| docker-compose | **AUCUN** fichier dans le repo | recherche exhaustive |
| CI/CD | **CI/CD NON IDENTIFIÉ** — pas de `.github/workflows`, aucun pipeline ; `.github/modernize` = artefacts outil java-upgrade, pas de CI | arborescence |
| Build frontend | `vite build`, Node >=20 | package.json |
| Profils Spring | dev (debug/swagger ON), prod (swagger OFF, actuator health+info, erreurs masquées) | application-{dev,prod}.properties |

Processus réel : push main → déploiement manuel/plateforme (Railway redéploie le Docker backend ; Vercel build la SPA). Migrations Flyway exécutées au démarrage du backend.

## 21. Technologies and Versions

### Langages
- **Java 21** (backend, pom.xml) · **TypeScript ~5.5.3** (frontend) · **SQL PostgreSQL** (migrations)

### Backend
| Techno | Version | Rôle | Source |
|---|---|---|---|
| Spring Boot | **3.5.2** (parent) | Framework API REST | pom.xml |
| Spring Security + spring-security-test | (Boot) | AuthN/AuthZ, filtres | pom.xml |
| Spring Data JPA / Hibernate | (Boot) | Persistance, Specifications | pom.xml |
| Spring WebSocket | (Boot) | Notifications temps réel | pom.xml |
| Spring Mail + Thymeleaf | (Boot) | Emails transactionnels | templates/emails |
| Spring Actuator + springdoc-openapi **2.8.9** | Santé + Swagger (/v3/api-docs) | pom.xml |
| **JJWT 0.12.7** | JWT HS256 | pom.xml |
| **Flyway** (+ flyway-database-postgresql) | Migrations | pom.xml |
| PostgreSQL driver | SGBD | pom.xml |
| MapStruct 1.6.3 / Lombok 1.18.46 | Mappers DTO / boilerplate | pom.xml (annotation processors) |
| Bucket4j 8.10.1 | Rate limiting | pom.xml |
| Caffeine | Cache local | pom.xml |
| Apache POI, Commons-CSV, OpenPDF | Exports rapports | pom.xml |

### Frontend
| Techno | Version | Rôle |
|---|---|---|
| react / react-dom | ^18.3.1 | UI |
| react-router-dom | ^7.18.1 | Routing |
| @tanstack/react-query | ^5.101.4 | Server state/cache |
| axios | ^1.18.1 | Client HTTP |
| react-hook-form + @hookform/resolvers | ^7.83 / ^5.5.7 | Formulaires |
| zod | ^3.25.76 | Validation schémas |
| tailwindcss | ^3.4.1 | Styling (dark mode class) |
| lucide-react | ^0.344.0 | Icônes |
| dompurify | ^3.4.12 | Sanitization HTML (markdown IA) |
| vite | ^5.4.21 | Build/dev server (proxy /api→8081) |
| typescript-eslint, eslint 9, autoprefixer, postcss | Dev quality |

⚠️ `@supabase/supabase-js ^2.57.4` est présent dans package.json mais **aucun usage trouvé dans src/** — dépendance morte probable (`À CONFIRMER AVEC LE STAGIAIRE`, à retirer ou justifier).

### Base de données
PostgreSQL (prod, managé Railway — version exacte non précisée dans le repo : `À CONFIRMER`) ; H2 MODE=PostgreSQL (tests).

### Sécurité
BCrypt(10), JWT HS256, Bucket4j, CSP/HSTS, DOMPurify, XSSFilter.

### IA
Google Gemini API (`gemini-2.5-flash`) ; Groq API (`openai/gpt-oss-120b`) ; orchestration fallback maison.

### DevOps
Docker (multi-stage), Railway (backend), Vercel (frontend). Git (main unique).

### Tests
JUnit 5 + Mockito + Spring Test/H2 (backend). **Aucun framework front**.

### Outils
Maven Wrapper, ESLint 9, tsc --noEmit, Qodo (config vide), scripts utilitaires racine (fix-imports.js…).

## 22. External Services

| Service | Usage | Variables d'environnement (noms seulement) |
|---|---|---|
| Google Gemini API | Génération IA primaire | GEMINI_API_KEY, GEMINI_MODEL |
| Groq API | IA secondaire/fallback + chat | GROQ_API_KEY, GROQ_MODEL |
| Brevo | Emails SMTP relay (smtp-relay.brevo.com:465) ou API REST si clé présente | MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, BREVO_API_KEY, MAIL_FROM_ADDRESS, MAIL_FROM_NAME |
| Railway | Hébergement backend + PostgreSQL managé | DATABASE_URL, SPRING_DATASOURCE_URL/USERNAME/PASSWORD |
| Vercel | Hébergement frontend | VITE_API_BASE_URL, VITE_API_TIMEOUT (+VERCEL_OIDC_TOKEN généré CLI) |
| — | Aucun stockage cloud (LOCAL), aucun OAuth externe, aucun paiement | — |

Autres variables backend : JWT_SECRET, ACTIVATION_BASE_URL, CORS_ORIGIN, STORAGE_PATH, MAX_FILE_SIZE, app.login-security.* (fichier), CSP connect-src.

## 23. UI Pages

Pages publiques : `/login`, `/forgot-password`, `/activate[/{success|invalid|expired}]`, `/reset-password[...]`, `/session-expired`, `/401`, `/403`, `/404`.

| Page | Route | Rôle autorisé | Fonctionnalités clés |
|---|---|---|---|
| Dashboard (dynamique) | /app/dashboard | Tous (rendu selon rôle) | AdminDashboardPage si admin ; département si manager ; sinon membre |
| Personal dashboard | /app/personal-dashboard | Tous | Mes tâches, stats personnelles |
| Workspaces (admin) | /app/workspace-overview, all-workspaces, create/edit-workspace, workspace-members, -activity, -analytics, -reports, archived-workspaces | SUPER_ADMIN/ADMIN | Gestion complète des tenants |
| Projets | /app/projects, /app/projects/:id, /app/archived-projects | Membres ws | CRUD, modals create/edit/archive/restore |
| Tâches | /app/tasks (?ws=&dept=&proj=), /app/tasks/:taskId, /app/collaboration | Membres | Liste filtrée paginée, détails (checklists, commentaires, PJ, mentions), collaboration |
| Départements | /app/departments, /app/departments/:id?tab=… | Manager+/Admin | Onglets par type : hr (12 onglets), development (sprints/tâches), cybersecurity (audits), marketing (campagnes), ai (modèles) + onglets communs (overview/docs/reports/analytics/activity/settings) |
| Équipes / Membres | /app/teams, /app/members[/:memberId] | Membres | Détails équipe, annuaire |
| Organisation | /app/organization | Admin | Vue structure |
| Documents & KB | /app/documents[/:documentId], /app/knowledge | Membres | Upload, preview (file-preview), approbation, versions ; articles KB |
| Handover | /app/handover, /app/handover-entries (+ détail) | Membres | Workflow passation, journaux |
| Communication | /app/communication (+ conversations, chat/:id, direct-messages, announcements, search, files) | Membres | Canaux, DM, annonces, recherche, fichiers partagés |
| IA | /app/ai (index dashboard, prompts, history, analytics, handover, knowledge, reports, conversations/:id, ai/report/:reportId) | Manager+ pour génération | Chat IA streaming UI, prompt library, report viewer (insights/charts/recommendations/sources) |
| Productivité | /app/notifications, /app/alerts, /app/reports, /app/activity, /app/calendar | Tous | Centre de notifications, alertes, rapports (builder/history/templates/export/PDF), timeline, calendrier |
| Reporting & Analytics | pages/productivity/Reporting & Analytics/* (AnalyticsPage, Performance/Productivity/Workload, ReportBuilder/Details/History/Templates, ExportCenter, PDFPreview) | Manager+/Admin | Analytics multi-axes, exports |
| Profil | /app/profile (index my-profile, account, security, preferences, notifications, sessions, activity) | Tous | Profil, sécurité, sessions actives, préférences |
| Paramètres | /app/settings | WORKSPACE_UPDATE (guard) | Settings workspace + personnels |
| Administration | /app/admin/users[/:userId], roles[/:roleId], permissions, audit-logs | SUPER_ADMIN/ADMIN | CRUD users (modals create/edit/détails), rôles/permissions, logs |

Pages prioritaires pour captures (chapitre Réalisation) : Login, Dashboards (les 3 variantes), Projects+TaskDetails, Department HR tabs, Handover detail, Communication chat, AI conversation/report viewer, Administration users.

## 24. Recommended Screenshots

```text
Figure 4.1 — Page de connexion
Objectif : démontrer l'authentification JWT
Éléments : formulaire zod validé, bouton loading
Utilisateur : compte actif standard
Fonctionnalité : UC-1

Figure 4.2 — Activation de compte (email → formulaire)
Éléments : ActivationForm + indicateur force mot de passe
Fonctionnalité : cycle de vie utilisateur

Figure 4.3 — Dashboard Administrateur
Éléments : AdminDashboardStats + charts (statuts projets, activité)
Utilisateur : admin
Endpoints : GET /dashboard/workspace, /analytics/admin/*

Figure 4.4 — Dashboard Membre (personnel)
Endpoints : GET /dashboard/me
Utilisateur : member

Figure 4.5 — Liste des projets + création (modal)
Éléments : filtres, couleurs/icônes, pagination
Endpoints : ProjectController POST/GET

Figure 4.6 — Détail tâche (vue la plus riche)
Éléments : statut/priorité, checklist cochable, commentaires, pièces jointes, mentions @, activités
Endpoints : Task/Checklist/Comment/Attachment/Mention controllers

Figure 4.7 — Département RH — Candidats et Entretiens
Éléments : pipeline candidats (statuts), drawer entretien, calendrier entretiens
Utilisateur : manager RH
Endpoints : CandidateController, InterviewCalendarController

Figure 4.8 — Employés / Présence / Évaluations (2 captures max)
Endpoints : EmployeeController, AttendanceController, PerformanceReviewController

Figure 4.9 — Sprints (développement) et Audits (cybersécurité) et Campagnes (marketing) — 1 capture chacun
Endpoints : Sprint/SecurityAudit/MarketingCampaign controllers

Figure 4.10 — Documents : upload + workflow d'approbation + prévisualisation
Endpoints : DocumentController upload/approve, file-preview components

Figure 4.11 — Passation (handover) : entrée + timeline + accept/reject
Endpoints : HandoverEntryController

Figure 4.12 — Messagerie : canal + messages + fichiers partagés
Endpoints : Conversation/Message controllers

Figure 4.13 — Notifications temps réel (badge topbar + panneau)
Éléments : notification reçue SANS rechargement (preuve WebSocket)

Figure 4.14 — Assistant IA : conversation + bibliothèque de prompts + rapport généré (ReportViewer)
Utilisateur : manager (scope projet)
Endpoints : /api/analytics/ai/generate etc.
Fonctionnalité : différenciateur majeur du PFA

Figure 4.15 — Administration : gestion utilisateurs (modales, actions cycle de vie) + rôles/permissions
Endpoints : UserController, RoleController

Figure 4.16 — Analytics / rapports exportables (builder + export PDF)
Endpoints : AnalyticsController, reporting exports
```

## 25. References Found in Project

Références techniques citables (vérifiées dans le repo) :
1. Spring Boot 3.5.2 Documentation — spring.io/projects/spring-boot (pom.xml parent).
2. React 18 Documentation — react.dev (package.json).
3. TanStack Query v5 — tanstack.com/query (package.json).
4. PostgreSQL Documentation — postgresql.org (driver, migrations).
5. Flyway Documentation — flywaydb.org (db/migration).
6. JJWT 0.12.x — github.com/jwtk/jjwt (pom.xml).
7. Bucket4j — bucket4j.com (pom.xml).
8. Vite 5 — vitejs.dev ; Tailwind CSS 3 — tailwindcss.com.
9. Google Gemini API documentation (ai.gemini.* config) ; Groq API documentation (ai.groq.*, API compatible OpenAI).
10. Brevo API/SMTP documentation (commits Brevo, application.properties).
11. Docs internes : docs/SECURITY_REPORT.md ; COLLABIX_BACKEND_AUDIT.md ; frontend-audit.md.
Références UML/méthodo (RUP, Merise…) : NON TROUVÉ DANS LE CODE — à choisir par le stagiaire.

## 26. Report ↔ Code Matrix

| Section du rapport | Information nécessaire | Trouvée ? | Source | À fournir manuellement ? |
|---|---|---|---|---|
| Pages préliminaires (garde, dédicace, remerciements, abstract, glossaire, listes, TDM) | Contexte humain/institutionnel | Non | — | **OUI** (générées à partir de ce doc + infos perso) |
| Introduction générale | Cadre du stage | Non | — | **OUI** |
| Ch.1 Organisme d'accueil, problématique, existant, planification, équipe | Histoire entreprise, outils gestion projet | Non (`À CONFIRMER PAR LE STAGIAIRE`) | — git projects/Jira/Trello trouvés | **OUI** |
| Ch.1 Méthodologie | Agile/Scrum/Kanban réels | `À CONFIRMER AVEC LE STAGIAIRE` | Seul indice : TODO.md structuré en "sprints" informels | OUI |
| Ch.2 Besoins & exigences fonctionnelles | Liste des fonctions | **OUI** | §3 Functional Modules | Reformulation rédactionnelle |
| Ch.2 Acteurs | Rôles/permissions | **OUI** | §4, RoleName.java, SecurityConfig | — |
| Ch.2 Cas d'utilisation | Scénarios | **OUI** | §11 | Enrichissement alternatifs |
| Ch.2 Diagrammes (classes, séquences, activités) | Données complètes | **OUI** | §6, §7, §12, §30 | Dessin des diagrammes |
| Ch.3 Architecture technique & applicative | Composants, flux | **OUI** | §8, §9, §13 | Schématisation propre |
| Ch.3 Backend/Frontend | Frameworks, patterns, versions exactes | **OUI** | §21, pom.xml/package.json | — |
| Ch.3 Base de données | Tables, relations, migrations | **OUI** | §6 (80 migrations Flyway) | MCD/MLD dessinés |
| Ch.3 Tests | Stratégie, résultats | **OUI** | §16 (72 tests verts partiels ; front = 0 test) | Décision d'assumer ou compléter les tests front |
| Ch.3 Déploiement | Docker/Railway/Vercel | **OUI** | §20 | Captures plateformes |
| Ch.4 Interfaces & scénarios | Pages + fonctionnalités | **OUI** | §23, §24 | Captures d'écran réelles |
| Conclusion & perspectives | Bilan, limites | Partiel | §19 résidus sécurité, §29 partiels | Bilan personnel |
| Références | Bibliographie | Partiel | §25 | Normes de citation de l'école |
| Annexes | Extraits code, schéma BD complet | **OUI** | ce document | Sélection d'extraits |

## 27. Information Missing From Code

# INFORMATIONS À ME FOURNIR PERSONNELLEMENT

| # | Information | Format attendu |
|---|---|---|
| 1 | Nom, historique, secteur, adresse de l'organisme d'accueil | Paragraphe descriptif |
| 2 | Encadrants (académique + entreprise) | Noms/fonctions |
| 3 | Dates et durée du stage | JJ/MM/AAAA → JJ/MM/AAAA (git suggère ~06/07/2026 → 21/08/2026 : confirmer) |
| 4 | Composition de l'équipe ("trio" ? 3 membres ? répartition backend/frontend ?) | Noms + rôles |
| 5 | Problématique terrain observée et objectifs donnés par l'entreprise | Texte |
| 6 | Méthodologie réellement suivie (Scrum ? sprints de combien ? réunions ? outil de suivi ?) | Description honnête |
| 7 | Difficultés rencontrées et solutions (ex. CORS Railway, SMTP Brevo, migration e-mail — visibles dans git mais contexte à raconter) | Anecdotes datées |
| 8 | Résultats métier / retours utilisateurs / nombre d'utilisateurs réels | Chiffres ou témoignages |
| 9 | Captures d'écran de toutes les figures listées §24 (comptes de démo admin/manager/member recommandés) | PNG propres |
| 10 | Version PostgreSQL réelle en prod, taille des données | Depuis Railway |
| 11 | Choix à assumer : tests frontend absents, CI/CD absent, dépendance supabase-js inutilisée, seed admin en dur | Décisions |
| 12 | Contexte PFA : année universitaire, encadrement, soutenance | Selon modèle de l'école |

## 28. Contradictions to Verify

```text
Élément : Nom du projet côté frontend
Source 1 : package.json name = "vite-react-typescript-starter" (template)
Source 2 : URLs prod + issuer JWT + audit docs = "Collabix"
Contradiction : nom template vs nom métier
Ce que le code indique réellement : le produit s'appelle Collabix ; le package.json garde le nom du starter
Décision à confirmer : renommer avant livraison (cosmétique)

Élément : README
Source 1 : README.md.txt (racine) = FICHIER VIDE
Source 2 : extension .txt inhabituelle
Contradiction : aucun readme réel alors qu'un projet fini devrait en avoir un
Ce que le code indique : pas de documentation d'installation utilisateur
Décision à confirmer : créer un README pour l'annexe ?

Élément : frontend-audit.md (26/07/2026)
Source 1 : affirme "routing custom sans React Router", "données 100% mockées"
Source 2 : code actuel = react-router-dom 7 + axios + vraie API partout
Contradiction : document obsolète vs état final
Ce que le code indique : l'audit décrit une version intermédiaire ; NE PAS citer ce document comme source du rapport sans le mettre à jour

Élément : Mot de passe admin seedé
Source 1 : migration V20260830 seed_initial_admin avec mot de passe par défaut connu (signalé dans COLLABIX_BACKEND_AUDIT.md)
Source 2 : docs/SECURITY_REPORT.md liste "mot de passe DB en dur" corrigé, mais le seed admin reste un risque si non changé en prod
Contradiction : posture "production ready" vs secret seedé
Ce que le code indique : le seed existe toujours en base
Décision à confirmer : changer le mot de passe admin en prod avant démonstration

Élément : URLs d'activation/reset
Source 1 : COLLABIX_BACKEND_AUDIT.md signale incohérence ports 5173 vs 4200 dans ACTIVATION_BASE_URL/examples
Source 2 : frontend réel tourne sur 5173 (Vite) / vercel.app en prod
Ce que le code indique : valeur pilotée par env ACTIVATION_BASE_URL ; vérifier la valeur en prod
Décision à confirmer avec le stagiaire

Élément : dossier database/ vide + diagrams/ vide
Source 1 : arborescence
Source 2 : tout le schéma est dans backend/db/migration
Ce que le code indique : dossiers placeholders ; ne pas présenter "database/" comme composant du projet
```

## 29. Partially Implemented Features

| Fonctionnalité | État | Ce qui existe | Ce qui manque | Fichiers |
|---|---|---|---|---|
| Threads de commentaires | Ébauche backend | `Comment.parentCommentId` (colonne simple, pas de relation JPA) | API/frontend de threads | entity/Comment.java |
| Expérience "Department Experience Polish" | **Backlog ouvert** (TODO.md racine, rien coché) | Onglets communs départements présents | Routage par type de dept, DeptOverview/Documents/Reports/Analytics avec vraies données, placeholders marketing à remplacer, onglets HR manquants (Reports/Analytics/Activity/Settings), suppression des "Coming soon" | TODO.md, pages/departments/common/*, departments/marketing/* |
| Tests frontend | Absent | ESLint + tsc | Aucun framework installé | package.json |
| CI/CD | Absent | Dockerfile + hébergements | Aucun workflow | .github/ |
| Purge/expiration automatique (tokens, notifications) | Absent | Repositories de purge existent (`findReadBefore`, `deleteAllByExpiresAtBefore`) | Aucun scheduler @Scheduled appelant ces méthodes | RefreshTokenRepository, NotificationRepository |
| Blacklist tokens au logout | Partiel | Refresh révoqué ; access reste valide jusqu'à expiration (5 h) | Denylist/jti check | docs/SECURITY_REPORT.md |
| Sanitization erreurs IA / prompt injection | Manquant | Pipeline robuste aux pannes | Filtrage injection, sanitization réponses | docs/SECURITY_REPORT.md |
| Stockage cloud documents | Non implémenté | Champ storageType prévu (LOCAL/S3/GCS/AZURE) | Seul LOCAL codé | entity/Document.java |
| Couverture de tests mesurée | Absent | 72 tests verts | JaCoCo non configuré | pom.xml |

## 30. Recommended UML Diagrams

1. **Diagramme de contexte** : Utilisateurs (4 rôles) ↔ Collabix ↔ {Gemini API, Groq API, Brevo, PostgreSQL} — composants §13/§22.
2. **Cas d'utilisation** (§11, §12.1) : acteurs Super Admin/Admin/Manager/Member + généralisation ; cas auth inclus partout ; 5 UC détaillés.
3. **Diagramme de classes** : noyau User/Role/Permission/Workspace/Department/Team/Project/Task + AuditAuditableEntity ; attributs prêts §6-§7.
4. **Séquences** (§12.3) : login JWT, refresh 401, création tâche, upload document + notification WS, génération IA analytics.
5. **Activités** (§12.4) : cycle de vie user, workflow document/handover/performance review.
6. **Architecture technique** : §13 (schéma prêt).
7. **Architecture applicative** : couches §8 + pattern services/hooks front §9.
8. **Modèle de données (MLD)** : §6.1-6.2 (60 tables, relations, cardinalités).

## 31. Final Technical Summary

Collabix est une **plateforme web collaborative multi-workspaces** réalisée en ~7 semaines (30 commits, main unique). Le backend est un **monolithe Spring Boot 3.5.2 (Java 21)** en architecture en couches stricte (57 contrôleurs REST ≈ 350 endpoints → services → repositories Spring Data → **PostgreSQL** dont le schéma est intégralement piloté par **80 migrations Flyway**, entités UUID auditées avec verrouillage optimiste, soft delete par statuts). La sécurité est le point fort du projet : **JWT HS256** access 5 h / refresh 7 j persistés et révocables, permissions fines RBAC embarquées dans le token, hiérarchie SUPER_ADMIN > ADMIN > MANAGER > MEMBER, scoping multi-tenant workspace/département à trois niveaux de défense, BCrypt(10), rate limiting Bucket4j, CSP/HSTS, anti brute-force. Le frontend est une **SPA React 18/TypeScript/Vite** avec TanStack Query, axios (refresh silencieux single-flight), react-hook-form+zod, design system interne Tailwind (29 composants, dark mode) et WebSocket natif pour les notifications temps réel. Les modules métier couvrent projets/tâches/checklists/commentaires/documents avec workflow d'approbation/base de connaissances/passation (handover), messagerie et annonces, dashboards et analytics multi-scope, quatre modules départementaux (RH/ATS complet, sprints dev, audits cyber, campagnes marketing) et un **module IA générative différenciant** orchestrant **Google Gemini (gemini-2.5-flash)** avec fallback **Groq** via un PipelineExecutor traçant chaque appel en base (ai_history), prompts versionnés en base, accès contrôlé par scope. Déploiement réel : **Docker (multi-stage) sur Railway** (backend + PostgreSQL managé) et **Vercel** (frontend), emails Brevo ; **aucune CI/CD**. Qualité : 24 classes de test backend (72 tests verts sur exécutions partielles, intégration E2E H2), zéro test frontend (lint + typecheck seulement), score auto-évalué de sécurité 84/100 avec vulnérabilités résiduelles documentées (prompt injection, audit logging). Restes ouverts : polish de l'expérience départements (TODO.md), purge automatique, threads de commentaires.

---

*Fin du document — généré par audit statique du repository. Toute donnée absente est marquée explicitement.*





