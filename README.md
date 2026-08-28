# Collabix

**Collabix** is an enterprise collaboration platform designed for small and medium-sized enterprises (SMEs). It
consolidates the tools a growing team needs into a single workspace: project and task management, department and team
coordination, document and knowledge-base management, HR workflows, internal communication, AI-assisted productivity,
reporting/analytics, and an administrative layer for users, roles and permissions.

> **Project status:** Internship / PFA (Projet de Fin d'Année) project.
> It is a feature-rich **MVP**, not a production-hardened system. Several modules are fully wired end-to-end while
> others are partially implemented. See [Project Status](#project-status) and [Known Limitations](#known-limitations).

---

## Table of Contents

1. [Overview](#overview)
2. [Project Status](#project-status)
3. [Technology Stack](#technology-stack)
4. [System Architecture](#system-architecture)
5. [Repository Structure](#repository-structure)
6. [Prerequisites](#prerequisites)
7. [Installation](#installation)
8. [Configuration](#configuration)
9. [Database Setup](#database-setup)
10. [Running the Backend](#running-the-backend)
11. [Running the Frontend](#running-the-frontend)
12. [Authentication](#authentication)
13. [Roles and Permissions](#roles-and-permissions)
14. [Main Modules](#main-modules)
15. [API Organization](#api-organization)
16. [AI Architecture](#ai-architecture)
17. [File Storage](#file-storage)
18. [Development Workflow](#development-workflow)
19. [Testing](#testing)
20. [Troubleshooting](#troubleshooting)
21. [Known Limitations](#known-limitations)
22. [Future Work](#future-work)
23. [Handover Notes](#handover-notes)

---

## Overview

Collabix addresses the problem of **fragmented collaboration**: in many SMEs, projects, documents, HR and communication
live in disconnected tools. Collabix brings them together under one authenticated workspace with a consistent
permission model.

What the platform currently provides (all backed by real backend controllers + frontend pages):

- **Authentication & accounts** — JWT access/refresh tokens, email-based account activation, password reset,
  brute-force login protection.
- **Workspaces** — top-level tenants; a user can belong to multiple workspaces with a per-workspace role.
- **Organization** — departments, teams, and members with department-scoped access.
- **Projects & Tasks** — project lifecycle, task assignment, sprints, kanban-style workflows, collaboration threads.
- **Documents** — upload/download/preview, versioning, workspace- and project-scoped document lists, approval flow.
- **Knowledge Base** — categorized articles.
- **Handover Journal** — structured handover entries, attachments, comments and timelines (commonly used by
  departments such as IT/cybersecurity).
- **Communication** — real-time chat (channels + direct messages), announcements, shared files, message search.
- **AI** — chat assistant plus domain AI features (analytics summaries, handover drafting, knowledge answers, report
  generation) powered by external LLM providers (Google Gemini and Groq).
- **HR** — employees, candidates, interviews, onboarding, performance reviews, attendance, skills.
- **Cybersecurity** — security audit records.
- **Marketing** — campaigns.
- **Reporting & Dashboards** — workspace/project analytics, executive reports, activity center, calendar, alerts and
  notifications.
- **Administration** — global user management, roles, granular permissions, and audit logs.

Only features that exist in the codebase are listed above.

---

## Project Status

| Area | Status | Notes |
|------|--------|-------|
| Auth (JWT, activation, reset, refresh) | Implemented | End-to-end; backend + frontend. |
| Workspaces / Departments / Teams / Members | Implemented | Department-scoped access is enforced. |
| Projects / Tasks / Sprints | Implemented | Backend + frontend pages exist. |
| Documents (upload, list, view, download, version, approval) | Implemented | Upload & view were recently fixed (see Troubleshooting). |
| Knowledge Base | Implemented | |
| Handover Journal | Implemented | |
| Communication (chat, DM, announcements) | Implemented | WebSocket-backed. |
| AI (chat, prompts, history, domain features) | Implemented (backend) + UI | Requires external API keys to function. |
| HR (employees, candidates, interviews, onboarding, performance, attendance) | Implemented (backend) + department tabs | UI lives under department tabs. |
| Cybersecurity / Marketing modules | Implemented (backend) + partial UI | |
| Reporting / Analytics / Dashboards | Implemented | Maturity varies; treat as MVP. |
| Administration (users/roles/permissions/audit) | Implemented | |
| Notifications / Alerts | Implemented | In-app + email. |
| Frontend automated tests | **Not implemented** | Only `lint` + `typecheck` are configured. |
| Production hardening (object storage, distributed cache, CI) | Not implemented | See Known Limitations. |

**Do not assume any module is production-ready.** Run the app and the test suite, and review per-module code before
extending a feature.

---

## Technology Stack

| Layer | Technology |
|------|------------|
| Frontend | React 18.3, TypeScript 5.5, Vite 5.4, React Router 7.18, TanStack Query 5, Tailwind CSS 3.4 |
| Frontend state/forms | Axios (custom client), Zod + React Hook Form, `@tanstack/react-query` |
| Backend | Spring Boot 3.5.2, Java 21 |
| Security | Spring Security, JJWT 0.12.7 (JWT), custom `PermissionEvaluator` |
| ORM | Hibernate / Spring Data JPA |
| Database (prod) | PostgreSQL |
| Database (tests) | H2 (in-memory) |
| Migrations | Flyway (78 migrations in `db/migration`) |
| API | REST (JSON), Spring MVC; OpenAPI 3 via SpringDoc 2.8.9 |
| Mapping / boilerplate | MapStruct 1.6.3, Lombok 1.18.46 |
| AI | Gemini (`generativelanguage.googleapis.com`) + Groq (`api.groq.com`) over Java `RestClient` (no Spring AI) |
| Caching | Spring Cache + Caffeine |
| Realtime | Spring WebSocket |
| Email | Spring Mail (Brevo SMTP/API or generic SMTP) |
| Export | Apache POI 5.3 (Excel), OpenPDF 1.4.1 (PDF), Commons CSV 1.11 |
| Rate limiting | Bucket4j 8.10.1 |
| Observability | Spring Actuator |
| Build (backend) | Maven (`./mvnw`) |
| Build (frontend) | npm + Vite |
| Storage | Local filesystem (`./uploads`) via `StorageService` (pluggable) |
| Deploy targets (per config) | Backend: Railway (Dockerfile provided); Frontend: Vercel |

Versions are taken from `backend/pom.xml`, `frontend/project/package.json` and `application.properties`. Do not assume
other versions.

> **Note:** `frontend/project/package.json` lists `@supabase/supabase-js`, but it is **not used** anywhere in the
> source (the app talks to the Spring backend over REST). Do not treat Supabase as the data layer.

---

## System Architecture

```
Browser (React SPA)
      │  REST/JSON + Bearer JWT        ▲  WebSocket (chat/notifications)
      ▼                                │
Vite dev proxy (/api, /ws)  ──────────►  Spring Boot (port 8081)
                                        │
                                        ├─ Controllers (REST, OpenAPI)
                                        ├─ Security (JWT filter, Workspace/Department authorization, PermissionEvaluator)
                                        ├─ Service layer (business logic, department scoping, AI orchestration)
                                        ├─ Repository layer (Spring Data JPA)
                                        ▼
                                   PostgreSQL  (Flyway-managed schema)
                                        │
                  External services ───┴──►  Gemini / Groq (AI), Brevo/SMTP (email)
```

**Flow of a request**

1. The React SPA calls the backend through a typed Axios client (`lib/api.ts`). In dev, Vite proxies `/api` →
   `http://localhost:8081` and `/ws` → `ws://localhost:8081`.
2. A `JwtAuthenticationFilter` validates the `Authorization: Bearer <token>` header and populates the Spring
   `SecurityContext`.
3. Controllers are protected with `@PreAuthorize` SpEL expressions that combine:
   - workspace authorization (`@workspaceAuth.canViewWorkspace / canUpdateWorkspace / canDeleteWorkspace`),
   - department authorization (`@departmentAuth.canViewDepartment / canManageDepartmentProjects / canManageDepartmentHR`),
   - a permission check (`@permissionEvaluator.hasPermission(authentication, 'PERMISSION_CODE')`).
4. The service layer enforces **department scoping** a second time via `DepartmentScopeGuard` (defense in depth), then
   talks to repositories.
5. Responses are wrapped in an `ApiResponse<T>` envelope (`{ success, message, data, ... }`); Spring `Page` results are
   normalized by the frontend into `{ content, page }`.
6. Domain AI calls go through `AIOrchestratorService` → `GeminiService` / `GroqService` → external LLM APIs.

**Key architecture facts (verified in code)**

- The backend is a **single Spring Boot application**; there is no separate Python/AI microservice. AI is invoked from
  Java over HTTP. (`translate_french.py` at the repo root is a one-off code-translation helper, **not** a runtime
  service.)
- Documents are **project-scoped** (FK `project_id NOT NULL`, `ON DELETE CASCADE`) and also exposed via
  workspace-scoped endpoints (`WorkspaceDocumentController`).
- File contents are stored on the local filesystem; only metadata lives in the database.

---

## Repository Structure

```
collabix/
├── backend/                      # Spring Boot Maven application
│   ├── pom.xml
│   ├── Dockerfile                # Builds the backend jar (runs on 8081)
│   ├── .env.example              # Template for backend environment variables
│   ├── src/main/java/com/trio/backend/
│   │   ├── BackendApplication.java
│   │   ├── controller/           # REST controllers (auth, workspace, department, project, task,
│   │   │                         #   document, knowledge, handover, ai, hr, cyber, marketing, admin…)
│   │   ├── service/  service/impl/   # Business logic
│   │   ├── service/ai/ service/ai/impl/  # AI orchestration + providers
│   │   ├── security/             # jwt, workspace, department, permission, rate-limiting, xss
│   │   ├── repository/           # Spring Data JPA repositories
│   │   ├── entity/               # JPA entities (User, Workspace, Department, Project, Task, Document, …)
│   │   ├── dto/                   # Request/response DTOs
│   │   ├── mapper/               # MapStruct mappers
│   │   ├── storage/              # StorageService + LocalStorageServiceImpl
│   │   ├── reporting/analytics/   # Analytics/report builders
│   │   ├── websocket/            # Realtime endpoints
│   │   ├── config/  event/  exception/  validation/  util/
│   │   └── resources/
│   │       ├── application.properties     # Main config (reads env vars)
│   │       └── db/migration/              # Flyway SQL migrations (V…__.sql)
│   └── src/test/                 # JUnit 5 + MockMvc + SpringBootTest (H2)
├── frontend/
│   └── project/                  # The actual React app (Vite)
│       ├── package.json          # scripts: dev, build, lint, typecheck, preview
│       ├── vite.config.ts        # Dev proxy /api → 8081, /ws → 8081
│       ├── .env.example  .env.production
│       └── src/
│           ├── App.tsx           # Router + route table
│           ├── lib/               # api client, auth-context, theme, query-client
│           ├── pages/             # Feature pages (auth, dashboard, projects, tasks, knowledge,
│           │                      #   ai, communication, administration, departments/hr, …)
│           ├── components/        # UI + feature components (layout, ui, ai, knowledge, …)
│           ├── services/          # Typed API services + React Query hooks
│           ├── hooks/  types/
│           └── index.html
├── docs/
│   └── SECURITY_REPORT.md        # Security review notes
├── .github/                      # (Claude Code "modernize" hooks — not part of the app)
└── README.md
```

The root also contains helper scripts used during development (`translate_french.py`, `convert-french-to-english.ps1`,
`remaining_french_words.txt`) — these are code-quality utilities, not application features.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 21 | Required by Spring Boot 3.5 (`java.version=21`). |
| Maven | 3.9+ | Use the bundled `./mvnw` (`./mvnw.cmd` on Windows). |
| Node.js | >= 20 | Required by Vite 5 / frontend (`engines.node >= 20`). |
| PostgreSQL | 14+ (recommended) | Needed to run the backend in dev/prod. Tests use H2, no Postgres required for `./mvnw test`. |
| Git | any | |

Optional: a Gemini and/or Groq API key to enable AI features.

---

## Installation

```bash
# 1. Clone
git clone <repo-url> collabix
cd collabix

# 2. Backend dependencies (Maven wrapper downloads everything)
cd backend
cp .env.example .env          # then fill in JWT_SECRET and (optionally) DB/AI/MAIL values
# (no separate install step — ./mvnw resolves dependencies)

# 3. Frontend dependencies
cd ../frontend/project
cp .env.example .env.local    # or just rely on the Vite proxy default
npm install
```

There is **no** `docker-compose.yml`. The backend ships a single `Dockerfile`.

---

## Configuration

The backend reads configuration from `application.properties`, which is almost entirely driven by **environment
variables** (with sensible local defaults). Copy `backend/.env.example` to `backend/.env` and fill values, or export
them in your shell.

### Backend environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `JWT_SECRET` | _(required)_ | HS256 signing secret. **Must be set** (≥ 32 chars) or the app fails to start. |
| `SPRING_DATASOURCE_URL` / `DATABASE_URL` | `jdbc:postgresql://localhost:5432/collabix_db` | JDBC URL. In Railway, set `SPRING_DATASOURCE_URL` from the injected `DATABASE_URL` (prefix with `jdbc:`). |
| `SPRING_DATASOURCE_USERNAME` / `DB_USERNAME` | `collabix_user` | DB user. |
| `SPRING_DATASOURCE_PASSWORD` / `DB_PASSWORD` | `Collabix@2026` | DB password. |
| `GEMINI_API_KEY` | _(empty)_ | Google Gemini key (AI). |
| `GEMINI_MODEL` | `gemini-2.5-flash` | Gemini model. |
| `GEMINI_URL` | `https://generativelanguage.googleapis.com` | Gemini endpoint. |
| `GROQ_API_KEY` | _(empty)_ | Groq key (AI). |
| `GROQ_MODEL` | `openai/gpt-oss-120b` | Groq model (`backend/.env.example` suggests `llama-3.3-70b-versatile`). |
| `GROQ_URL` | `https://api.groq.com` | Groq endpoint. |
| `CORS_ORIGIN` | `https://collabix-chi.vercel.app,https://collabix-production-eead.up.railway.app` | Comma-separated allowed CORS origins. |
| `CSP_CONNECT_SRC` | `http://localhost:5173` | Space-separated origins for `connect-src`. |
| `WEBSOCKET_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000,https://*.collabix.app,https://*.vercel.app,…` | WebSocket CORS. |
| `MAIL_HOST` / `MAIL_PORT` | `smtp-relay.brevo.com` / `465` | SMTP host/port (Brevo default). |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | _(empty)_ | SMTP credentials. |
| `MAIL_FROM_ADDRESS` / `MAIL_FROM_NAME` | `noreply@collabix.app` / `Collabix` | From address. |
| `BREVO_API_KEY` | _(empty)_ | If set, email is sent via Brevo REST API instead of SMTP. |
| `ACTIVATION_BASE_URL` | `http://localhost:5173` | Base URL used in account-activation emails. |
| `STORAGE_PATH` | `./uploads` | Local upload directory. |
| `MAX_FILE_SIZE` / `MAX_FILE_SIZE_MB` / `MAX_REQUEST_SIZE_MB` | `10485760` / `10MB` / `10MB` | Upload size limits. |
| `PORT` | `8081` | Server port. |
| `DASHBOARD_RECENT_LIMIT` | `10` | Dashboard "recent" count. |
| `SPRING_PROFILES_ACTIVE` | _(none)_ | e.g. `prod`. |
| `app.login-security.*` | enabled, 5 attempts, 30m lock | Brute-force protection tuning. |

### Frontend environment variables

`frontend/project/.env.example` (copy to `.env.local` for dev):

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_API_BASE_URL` | _(unset → `/api` in dev)_ | Backend base URL. In dev the Vite proxy handles it; in prod set the full backend URL (e.g. `https://<backend>/api`). |
| `VITE_API_TIMEOUT` | `30000` | Axios timeout (ms). |

`frontend/project/.env.production` already hardcodes the Railway backend URL for `vite build`.

---

## Database Setup

1. Create a PostgreSQL database and user, e.g.:
   ```sql
   CREATE DATABASE collabix_db;
   CREATE USER collabix_user WITH PASSWORD 'Collabix@2026';
   GRANT ALL PRIVILEGES ON DATABASE collabix_db TO collabix_user;
   ```
   (Or override the credentials via the `SPRING_DATASOURCE_*` / `DB_*` env vars.)
2. **Flyway migrations run automatically on backend startup** (`spring.flyway.enabled=true`,
   `baseline-on-migrate=true`). You do **not** run them manually.
3. `spring.jpa.hibernate.ddl-auto=validate` — the schema is owned by migrations, not Hibernate.
4. For a quick local run **without** PostgreSQL, you can override `SPRING_DATASOURCE_URL` to an H2 URL, but the
   production target is PostgreSQL.

> Tests do **not** need PostgreSQL: the test profile uses an in-memory H2 database
> (`src/test/resources/application.properties`) with Flyway disabled.

---

## Running the Backend

```bash
cd backend

# Set required env (example dev secret — use a strong value in real environments)
export JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-characters

# Run (defaults to port 8081)
./mvnw spring-boot:run
# Windows:
./mvnw.cmd spring-boot:run

# Or build a jar and run it
./mvnw clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

- API base: `http://localhost:8081/api`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Swagger UI: `http://localhost:8081/swagger-ui` (enabled by default; disable with
  `springdoc.swagger-ui.enabled=false` / `springdoc.api-docs.enabled=false`)
- Health: `http://localhost:8081/actuator/health`

---

## Running the Frontend

```bash
cd frontend/project
npm install
npm run dev          # http://localhost:5173 (Vite dev server)
```

The Vite dev server proxies:
- `/api` → `http://localhost:8081` (backend REST)
- `/ws`  → `ws://localhost:8081` (WebSocket)

So with both running, open `http://localhost:5173`, log in, and the SPA talks to the backend transparently.

Production build:

```bash
npm run build        # outputs to dist/; set VITE_API_BASE_URL to the backend URL first
npm run preview      # serve the built bundle locally
```

Frontend scripts: `dev`, `build`, `lint` (`eslint .`), `typecheck` (`tsc --noEmit -p tsconfig.app.json`), `preview`.

---

## Authentication

- **Strategy:** Stateless JWT. `AuthController` issues an **access token** and a **refresh token**. The SPA stores them
  in `localStorage` under the key `collabix_auth`.
- **Refresh:** The Axios interceptor automatically calls `POST /api/auth/refresh` on a `401` and retries the original
  request; on failure it clears auth and emits a `session-expired` event (the app shows the Session Expired page).
- **Account activation:** New users receive an activation link (`ACTIVATION_BASE_URL`) → `ActivationController`.
- **Password reset:** `ForgotPasswordPage` → email link → `ResetPasswordPage`.
- **Brute-force protection:** `RateLimitingFilter` + `app.login-security.*` (max attempts, lock duration, automatic
  unlock). The rate limiter is **in-memory / per-instance** (see Known Limitations).
- **Transport:** All protected routes send `Authorization: Bearer <accessToken>`.

---

## Roles and Permissions

Two independent role dimensions exist:

**Global roles** (`RoleName`): `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `MEMBER` — applied to the user account.

**Workspace roles** (`WorkspaceRole`): `OWNER`, `ADMIN`, `MANAGER`, `MEMBER` — per workspace membership.

**Permissions** are code strings (e.g. `DOCUMENT_READ`, `DOCUMENT_UPLOAD`, `DOCUMENT_UPDATE`, `DOCUMENT_DELETE`,
`WORKSPACE_UPDATE`, `WORKSPACE_DELETE`, `PROJECT_CREATE`, `PROJECT_UPDATE`, `PROJECT_DELETE`,
`EMPLOYEE_DOCUMENT_READ/UPLOAD/UPDATE/DELETE`, `ATTACHMENT_UPLOAD`, `CANDIDATE_ATTACHMENT_UPLOAD`, …). They are assigned
to roles via `RolePermission` and checked at runtime by `PermissionEvaluator.hasPermission(...)`.

**Department scoping** is a critical concept: a non-admin workspace member can normally only operate on their
**primary department** (`User.primaryDepartment`). This is enforced both in `@PreAuthorize` SpEL
(`@departmentAuth.canViewDepartment`, `@workspaceAuth.canViewWorkspace`, …) and again in the service layer via
`DepartmentScopeGuard.assertDepartmentAccessible(...)`. Workspace OWNER/ADMIN (and global ADMIN/SUPER_ADMIN) bypass
department scoping.

> **Common pitfall:** a manager/member with `DOCUMENT_UPLOAD` could still get `403` if the target project's department
> is not their primary department. Department scoping is intentional, not a bug.

---

## Main Modules

| Module | Backend entry points | Frontend |
|--------|----------------------|----------|
| Auth / Users | `AuthController`, `UserController`, `AdminUserController`, `ActivationController`, `RefreshTokenServiceImpl`, `PasswordResetServiceImpl` | `pages/auth/*`, `pages/Administration/Users Management/*`, `pages/profile/*` |
| Workspaces | `WorkspaceController`, `WorkspaceMember` | `pages/Workspace*`, `pages/CreateWorkspacePage` |
| Departments / Teams / Members | `DepartmentController`, `TeamController`, `TeamMemberController` | `pages/departments/*`, `pages/teams/*`, `pages/members/*` |
| Projects / Tasks / Sprints | `ProjectController`, `TaskController`, `SprintController` | `pages/projects/*`, `pages/tasks/*`, `pages/projects/ProjectDetailsPage` |
| Documents | `DocumentController` (project-scoped), `WorkspaceDocumentController` (workspace-scoped), `DocumentServiceImpl` | `pages/knowledge/components/DocumentsPage`, `DocumentDetailPage` |
| Knowledge Base | `KnowledgeBaseController` | `pages/knowledge/components/KnowledgeBasePage` |
| Handover Journal | `HandoverJournalController`, `HandoverEntryController`, `HandoverJournalAccessController` | `pages/knowledge/components/HandoverJournalPage`, `pages/handover/*` |
| Communication | `ConversationController`, `MessageController`, `AnnouncementController`, `WorkspaceMessageController`, WebSocket | `pages/communication/*` |
| AI | `AIModelController`, `AIHistoryController`, `AIPromptController`, `AnalyticsAIController`, `HandoverAIController`, `KnowledgeAIController`, `ReportingAIController`, `service/ai/*` | `pages/ai/*` |
| HR | `EmployeeController`, `CandidateController`, `InterviewController`, `OnboardingController`, `PerformanceReviewController`, `AttendanceController`, `EmployeeSkillController`, `EmployeeDocumentController` | department tabs under `pages/departments/hr/*` |
| Cybersecurity | `SecurityAuditController` | `pages/departments/cybersecurity/*` |
| Marketing | `MarketingCampaignController` | department tabs (`marketing-campaign-service.ts`) |
| Reporting / Dashboards | `DashboardController`, `AnalyticsController`, `ReportingAIController`, `reporting/analytics/*` | `pages/DashboardPage`, `pages/ReportsPage`, `pages/workspace/WorkspaceAnalyticsPage` |
| Administration | `RoleController`, `PermissionController`, `AuditLogsController`, `SecurityAuditController` | `pages/Administration/*` |
| Notifications / Alerts | `NotificationController`, `NotificationPreferenceController`, `AlertController` | `pages/productivity/Notifications/*`, `pages/productivity/Alerts/*` |
| Activity / Calendar | `ActivityController` | `pages/activity/*`, `pages/calendar/*` |

---

## API Organization

- Base path: `/api`
- Documents are nested under workspaces/departments/projects:
  `/api/workspaces/{workspaceId}/departments/{departmentId}/projects/{projectId}/documents`
  and a workspace-wide list at `/api/workspaces/{workspaceId}/documents`.
- All responses use the `ApiResponse<T>` envelope. Paginated endpoints return a Spring `Page`
  (`content`, `pageable`, `totalPages`, …) which the frontend normalizes to `{ content, page }`.
- OpenAPI docs are generated at `/v3/api-docs`; Swagger UI at `/swagger-ui`.
- Errors return `ApiResponse` with a `message` and optional `errors` (field errors). The frontend normalizes these
  into a `NormalizedApiError` (`{ message, status, fieldErrors, raw }`).

---

## AI Architecture

There is **no Python service and no Spring AI dependency**. AI is implemented in Java:

- `AIOrchestratorService` routes a prompt to a provider.
- `GeminiServiceImpl` and `GroqServiceImpl` call the respective REST APIs using Spring's `RestClient`
  (`RestClientConfig`, `AIConfiguration`).
- `AIModel` entities let operators configure which provider/model is active; `AIPrompt` stores reusable prompt
  templates with categories; `AIHistory` stores past runs.
- Domain AI controllers expose features: analytics summaries (`AnalyticsAIController`), handover drafting
  (`HandoverAIController`), knowledge answers (`KnowledgeAIController`), and report generation
  (`ReportingAIController`). The frontend exposes these under `pages/ai/*` (dashboard, prompt library, history,
  conversations, and per-domain pages).
- `security/ai/AIScopeAuthorization` guards AI usage per workspace/scope.

**Without `GEMINI_API_KEY` / `GROQ_API_KEY` the AI features will fail at call time** — the rest of the app is
unaffected.

---

## File Storage

- Implemented by `StorageService` → `LocalStorageServiceImpl`.
- Uploaded files are written to `app.storage.local.path` (default `./uploads`) using a generated storage name; only
  metadata (`fileName`, `mimeType`, `fileSize`, `storagePath`) is persisted in `Document`.
- Download streams the file back from storage (`DocumentController.download`).
- Max upload size is `10MB` (Spring `multipart.max-file-size` + `app.storage.max-file-size`).
- **Validation:** `FileValidationService` accepts a file if either its detected MIME type **or** its extension is on
  the allow-list (images, PDF, Office docs, CSV/JSON/XML, ZIP, …).

> Local storage is **not** suitable for ephemeral cloud deployments (files are lost on container restart). See Known
> Limitations.

---

## Development Workflow

1. Start PostgreSQL (or rely on H2 for tests only).
2. Start the backend (`backend/` → `./mvnw spring-boot:run`).
3. Start the frontend (`frontend/project` → `npm run dev`).
4. Open `http://localhost:5173`.
5. Use `npm run lint` and `npm run typecheck` before committing frontend changes.
6. Backend: rely on `./mvnw test` and the compiler; MapStruct/Lombok run as annotation processors.

Branching/conventions are not enforced by tooling in this repo — agree a convention with the new team. There is no
CI pipeline configured.

---

## Testing

**Backend** (`backend/`)

- JUnit 5 + `MockMvc` + `@SpringBootTest` integration tests under `src/test`.
- Tests run against an **in-memory H2** database (`src/test/resources/application.properties` sets
  `spring.datasource.url=jdbc:h2:mem:collabix_test` and disables Flyway), so **no PostgreSQL is required** to run them.
- Run all tests:
  ```bash
  cd backend
  ./mvnw test
  ```
- Example module test: `DocumentModuleE2EIntegrationTest` (covers project + workspace document upload/list/view/download
  and department isolation). Other isolation tests: `ProjectDepartmentIsolationIntegrationTest`,
  `TaskAssignmentIsolationIntegrationTest`, `TaskKanbanWorkflowIntegrationTest`, `AIScopeIntegrationTest`,
  `AlertModuleIntegrationTest`.

**Frontend** (`frontend/project`)

- There is **no unit/component test framework** configured (no Vitest/Jest in `package.json`).
- Quality gates are `npm run lint` (ESLint) and `npm run typecheck` (`tsc --noEmit`).
- Add a test runner (Vitest is the natural fit for Vite) if you want frontend coverage — see Future Work.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---------|--------------------|
| Backend won't start: `JWT_SECRET` / bean creation error | `JWT_SECRET` is required. Set a ≥32-char secret in `.env` or env. |
| `Could not resolve placeholder 'app.security.csp.connect-src'` | Set `CSP_CONNECT_SRC` (e.g. `http://localhost:5173`). The test profile needs it too. |
| `403 Forbidden` on documents/projects | Department scoping or missing permission. Confirm the user's primary department and that the role has the required `PERMISSION_CODE` (e.g. `DOCUMENT_UPLOAD`). |
| "Failed to load document / could not be found or you do not have access" when opening a doc | The document detail URL must include the correct `dept`/`proj`. This was fixed by including `departmentId` in `DocumentResponse` and deriving the detail path from it. Ensure you are on a build that includes that fix. |
| Upload succeeds but doc doesn't appear / can't be opened | Same root cause as above (department id was missing from the open link). Pull the fix and hard-refresh. |
| CORS / WebSocket connection errors in dev | Check `CORS_ORIGIN`, `CSP_CONNECT_SRC`, `WEBSOCKET_ALLOWED_ORIGINS` match the frontend origin (`http://localhost:5173`). |
| AI features return errors | Missing `GEMINI_API_KEY` / `GROQ_API_KEY`, or invalid model name. The rest of the app is unaffected. |
| Docker container not reachable on 8080 | The `Dockerfile` runs the jar on **8081** (`server.port=${PORT:8081}`); the `EXPOSE 8080` line is misleading. Run with `-e PORT=8080` or publish 8081. |
| Uploads disappear after deploy restart | Local `./uploads` storage is ephemeral on platforms like Railway. Switch to object storage. |
| French comments in code | Some source comments are still in French (`remaining_french_words.txt` lists leftovers). Use `translate_french.py` / `convert-french-to-english.ps1` to assist translation. |

---

## Known Limitations

- **Local file storage only** — not durable across restarts on ephemeral hosts; no S3/GCS abstraction wired in yet.
- **In-memory rate limiting & caching** — `RateLimitingFilter` (Bucket4j) and Caffeine cache are per-instance; they do
  not scale across multiple backend replicas.
- **No frontend test suite** — only lint + typecheck.
- **Uneven module maturity** — built as an MVP; some pages/features are partial. Verify before relying on a module.
- **AI depends on external keys** — Gemini/Groq must be configured; otherwise AI calls fail.
- **`Flyway validate-on-migrate=false`** — migrations won't fail on schema drift; keep migrations clean.
- **No CI/CD pipeline** is configured in the repo.
- **`@supabase/supabase-js` is an unused dependency** in the frontend.
- **Department scoping is strict** — non-admin users are limited to their primary department; this is by design but can
  surprise during testing.

---

## Future Work

- Object storage backend for `StorageService` (S3 / GCS / Azure) so uploads survive restarts.
- Distributed cache + rate limiting (Redis) for multi-instance deployments.
- Frontend test framework (Vitest + Testing Library) and a CI pipeline (GitHub Actions).
- Complete partially-implemented modules and polish UI consistency.
- Internationalization / removal of remaining French comments.
- Hardening: secrets management, audit logging breadth, input sanitization review (`XSSFilter` exists but verify
  coverage), and a production CORS/CSP policy review.
- Deployment manifest(s) (docker-compose / Kubernetes) if multi-service orchestration is needed.

---

## Handover Notes

- **Main backend class:** `com.trio.backend.BackendApplication` (entry point for `spring-boot:run` / jar).
- **Running tests without a database:** `cd backend && ./mvnw test` uses H2 — no Postgres needed.
- **Env templates:** `backend/.env.example` and `frontend/project/.env.example` (copy to `.env` / `.env.local`).
- **Security review:** see `docs/SECURITY_REPORT.md`.
- **Known DB-org TODOs:** `backend/TODO.md` and `backend/TODO_db_organisation_postgresql.sql`.
- **French-comment cleanup:** `remaining_french_words.txt`, `translate_french.py`, `convert-french-to-english.ps1`
  (root-level helpers).
- **API exploration:** start the backend and open `http://localhost:8081/swagger-ui` — the OpenAPI spec is the most
  reliable contract for endpoints and DTOs.
- **Department scoping** is the single most important security concept to understand before changing document/project
  endpoints — read `security/department/DepartmentScopeGuard.java` and `security/workspace/WorkspaceAuthorization.java`.
- **Recent fixes you should be aware of:** the document **upload permission** (managers/members with `DOCUMENT_UPLOAD`
  can now upload to their department) and the **document open/view** path. The root cause of "Failed to load document"
  was that `DocumentResponse.projectId` (and `departmentId`) were not populated by the MapStruct mapper, so the
  frontend built an open link with an empty `proj`/`dept`, disabling the detail query. `DocumentMapper.toResponse` now
  maps both `projectId` and `departmentId` from the document's project, and
  `pages/knowledge/components/DocumentsPage.tsx` derives the detail path from them. If you see regressions in document
  flows, check `DocumentMapper`, `DocumentController` (`getById`) and `DocumentsPage.tsx` first.

---

_Maintained as an internship/PFA project. Treat all "production" claims above as MVP-level until verified by the new
team._
