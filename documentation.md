# ClaimFlow — Insurance Claim Processing System (ICPS)

> **Version:** 1.1.0  
> **Updated:** 2026-09-19  
> **Stack:** Spring Boot 4.1 (Java 26) + React 19 (TypeScript) + PostgreSQL

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Roles & Permissions](#3-roles--permissions)
4. [Seeded User Accounts & Passwords](#4-seeded-user-accounts--passwords)
5. [Password Storage](#5-password-storage)
6. [Architecture Overview](#6-architecture-overview)
7. [Backend Module Breakdown](#7-backend-module-breakdown)
8. [Database Schema](#8-database-schema)
9. [API Endpoints Reference](#9-api-endpoints-reference)
10. [Claim Lifecycle & Statuses](#10-claim-lifecycle--statuses)
11. [Claim Types](#11-claim-types)
12. [Frontend Structure](#12-frontend-structure)
13. [Security & Authentication](#13-security--authentication)
14. [Configuration Reference](#14-configuration-reference)
15. [Running the Project](#15-running-the-project)
16. [Known Issues, Troubleshooting & Bug Resolutions](#16-known-issues-troubleshooting--bug-resolutions)
    - [Issue: PDF Uploaded by Customer Not Viewable or Downloadable by Staff and Admin](#issue-pdf-uploaded-by-customer-not-viewable-or-downloadable-by-staff-and-admin)

---

## 1. Project Overview

**ClaimFlow** (ICPS — Insurance Claim Processing System) is a full-stack enterprise web application designed to manage the end-to-end lifecycle of insurance claims. It supports multiple user roles — from customers filing claims to agents reviewing them, investigators conducting inquiries, supervisors overseeing the process, and administrators managing the entire system.

**Key Features:**
- Role-based access control (RBAC) with 5 distinct roles (`CUSTOMER`, `CLAIM_AGENT`, `INVESTIGATOR`, `SUPERVISOR`, `ADMIN`)
- Full claim lifecycle management across 9 workflow statuses
- Universal document upload, browser preview, and download (PDF, JPG, PNG, max 10 MB)
- Role-specific dashboards with live statistics and action queues
- 2-Stage authentication error alerting with automated fallback to Dummy Mode
- In-app notifications triggered on claim status transitions
- Immutable audit/activity timeline for every claim
- Advanced claim search with multi-filter support
- JWT-based stateless authentication
- Swagger/OpenAPI documentation at `/swagger-ui.html`

---

## 2. Technology Stack

### Backend
| Layer | Technology | Version |
|---|---|---|
| Language | Java | 26 |
| Framework | Spring Boot | 4.1.0 |
| Security | Spring Security + JWT | jjwt 0.12.6 |
| Persistence | Spring Data JPA / Hibernate | Latest |
| Database | PostgreSQL | 15+ |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.7.0 |
| Build Tool | Apache Maven | 3.9+ |
| Utilities | Project Lombok | 1.18.46 |
| Email | Spring Boot Mail (Mailtrap SMTP) | Latest |

### Frontend
| Layer | Technology | Version |
|---|---|---|
| Language | TypeScript | 5.7 |
| Framework | React | 19 |
| Bundler | Vite | 6.1 |
| Routing | React Router DOM | 7.3 |
| HTTP Client | Axios | 1.8 |
| State/Cache | TanStack React Query | 5 |
| Forms | React Hook Form + Zod | 7 / 3 |
| Charts | Recharts | 2 |
| Icons | Lucide React | Latest |
| Notifications | React Hot Toast | Latest |
| Styling | Tailwind CSS | 3.4 |

---

## 3. Roles & Permissions

There are **5 roles** in the system, defined in `Role.java`:

```
ADMIN | SUPERVISOR | CLAIM_AGENT | INVESTIGATOR | CUSTOMER
```

### Role Capability Matrix

| Capability | CUSTOMER | CLAIM_AGENT | INVESTIGATOR | SUPERVISOR | ADMIN |
|---|:---:|:---:|:---:|:---:|:---:|
| Register / Login | ✅ | ✅ | ✅ | ✅ | ✅ |
| View own profile | ✅ | ✅ | ✅ | ✅ | ✅ |
| Create/Edit DRAFT claim | ✅ | ❌ | ❌ | ❌ | ❌ |
| Submit claim | ✅ | ❌ | ❌ | ❌ | ❌ |
| Delete DRAFT claim | ✅ | ❌ | ❌ | ❌ | ❌ |
| Upload documents to claim | ✅ | ❌ | ❌ | ❌ | ❌ |
| **View & download claim documents (PDFs)** | ✅ (own) | ✅ (assigned/queue) | ✅ (assigned) | ✅ (all) | ✅ (system-wide) |
| View own claims | ✅ | ❌ | ❌ | ❌ | ❌ |
| View any claim (by ID) | ✅ (own) | ✅ | ✅ | ✅ | ✅ |
| Search claims (role-scoped) | ✅ (own) | ✅ | ✅ | ✅ | ✅ |
| View claims by status (work queue) | ❌ | ✅ | ✅ | ✅ | ❌ |
| Pick up claim for review | ❌ | ✅ | ❌ | ❌ | ❌ |
| Approve claim | ❌ | ✅ | ❌ | ✅ | ❌ |
| Reject claim | ❌ | ✅ | ❌ | ✅ | ❌ |
| Flag claim for investigation | ❌ | ✅ | ❌ | ❌ | ❌ |
| Start investigation | ❌ | ❌ | ✅ | ❌ | ❌ |
| Complete investigation | ❌ | ❌ | ✅ | ❌ | ❌ |
| Close claim | ❌ | ✅ | ❌ | ✅ | ❌ |
| Customer dashboard | ✅ | ❌ | ❌ | ❌ | ❌ |
| Agent dashboard | ❌ | ✅ | ❌ | ❌ | ❌ |
| Supervisor dashboard | ❌ | ❌ | ❌ | ✅ | ❌ |
| Admin dashboard stats | ❌ | ❌ | ❌ | ❌ | ✅ |
| List/manage all users | ❌ | ❌ | ❌ | ❌ | ✅ |
| Update any user's role | ❌ | ❌ | ❌ | ❌ | ✅ |
| Enable/disable user accounts | ❌ | ❌ | ❌ | ❌ | ✅ |
| List/view all claims (admin) | ❌ | ❌ | ❌ | ❌ | ✅ |
| View claim activity timeline | ✅ (own) | ✅ | ✅ | ✅ | ✅ |
| Receive in-app notifications | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 4. Seeded User Accounts & Passwords

The database seeder (`DatabaseSeeder.java`) executes non-destructively:
- If the database is empty, it initializes baseline production-like data (real staff and customer claims).
- If the database already contains users, it **preserves existing users** and ensures dummy accounts, dummy claim assignments, and sample documents exist without data loss.

### 1. Dummy Mode Accounts (For First-Time Visitors & Demo Testing)

| Role | Name | Email ID | Password | Notes |
|---|---|---|---|---|
| **Admin** | Dummy Admin | `dummy_admin@gmail.com` | `dummyadmin#@1234` | Full system access, all claims & PDF downloads |
| **Supervisor** | Dummy Supervisor | `dummy_supervisor@gmail.com` | `dummysupervisor#@1234` | Work queue oversight, approvals, and PDF reviews |
| **Claim Agent** | Dummy ClaimAgent | `dummy_claimagent@gmail.com` | `dummyclaimagent#@1234` | Pre-assigned claims in work queue, document viewer |
| **Investigator** | Dummy Investigator | `dummy_investigator@gmail.com` | `dummyinvestigator#@1234` | Inquiries, investigation notes, document reviews |
| **Customer** | Dummy Customer | `dummy_customer@gmail.com` | `dummycustomer#@1234` | Pre-seeded claims with uploaded documents |

### 2. Real Seeded Staff Accounts — Password: `Employee@Secure#2025`

| Role | Name | Email ID | Password |
|---|---|---|---|
| **Admin** | Vikram Sharma | `admin@icps.com` | `Employee@Secure#2025` |
| **Supervisor** | Arjun Verma | `sup1@icps.com` | `Employee@Secure#2025` |
| **Supervisor** | Rajesh Iyer | `sup2@icps.com` | `Employee@Secure#2025` |
| **Claim Agent** | Amit Singh | `agent1@icps.com` | `Employee@Secure#2025` |
| **Claim Agent** | Rahul Kumar | `agent2@icps.com` | `Employee@Secure#2025` |
| **Claim Agent** | Neha Gupta | `agent3@icps.com` | `Employee@Secure#2025` |
| **Claim Agent** | Priya Sharma | `agent4@icps.com` | `Employee@Secure#2025` |
| **Investigator** | Suresh Patel | `inv1@icps.com` | `Employee@Secure#2025` |
| **Investigator** | Ramesh Nair | `inv2@icps.com` | `Employee@Secure#2025` |
| **Investigator** | Manoj Desai | `inv3@icps.com` | `Employee@Secure#2025` |
| **Investigator** | Kiran Reddy | `inv4@icps.com` | `Employee@Secure#2025` |

### 3. Real Seeded Customer Accounts — Password: `Cust@Flow#2025`

| Role | Name | Email ID | Password | Seeded Claims |
|---|---|---|---|:---:|
| **Customer** | Aarav Das | `cust1@icps.com` | `Cust@Flow#2025` | 10 claims |
| **Customer** | Vihaan Rao | `cust2@icps.com` | `Cust@Flow#2025` | 8 claims |
| **Customer** | Aditya Menon | `cust3@icps.com` | `Cust@Flow#2025` | 7 claims |
| **Customer** | Sai Pillai | `cust4@icps.com` | `Cust@Flow#2025` | 6 claims |
| **Customer** | Ishaan Gowda | `cust5@icps.com` | `Cust@Flow#2025` | 5 claims |

---

## 5. Password Storage

All passwords are cryptographically hashed using **BCrypt** with salt rounds before storage in the database.
- **Encoder:** `BCryptPasswordEncoder`
- **Database Column:** `users.password` (contains `$2a$10$...` hash strings)
- Plaintext passwords are never persisted to the database or returned over API responses.

---

## 6. Architecture Overview

```
+-------------------------------------------------------------------+
|                     FRONTEND (React 19 + Vite)                    |
|   Port: 5173  *  TypeScript  *  Tailwind CSS  *  TanStack Query   |
|                                                                   |
|  Auth (2-Stage Alerts) | Customer Portal | Work Queue | Admin Ops |
+---------------------------------+---------------------------------+
                                  | REST API (JSON)
                                  | Header: Authorization: Bearer <JWT>
+---------------------------------v---------------------------------+
|                    BACKEND (Spring Boot 4.1)                      |
|   Port: 8080                                                      |
|                                                                   |
|  +---------------+ +---------------+ +---------------+            |
|  | Auth / Users  | | Claims / Search| | Work Queue    |            |
|  +---------------+ +---------------+ +---------------+            |
|  +---------------+ +---------------+ +---------------+            |
|  | Documents/PDF | | Notifications | | Admin / Audit |            |
|  +---------------+ +---------------+ +---------------+            |
|                                                                   |
|     Spring Security + Method-Level Security (@PreAuthorize)      |
+---------------------------------+---------------------------------+
                                  | JPA / Hibernate ORM
+---------------------------------v---------------------------------+
|                       PostgreSQL (icps_db)                        |
|   Port: 5432  *  Tables: users, claims, claim_documents, etc.    |
+-------------------------------------------------------------------+
```

---

## 7. Backend Module Breakdown

| Package | Key Classes | Functionality |
|---|---|---|
| `authentication` | `AuthController`, `AuthService`, `JwtService` | Registration, login token issuance, token verification |
| `user` | `UserController`, `UserService`, `User` entity | User profiles, account details |
| `claim` | `ClaimController`, `ClaimService`, `WorkflowController` | Claim lifecycle, status transitions, search |
| `document` | `DocumentController`, `DocumentService`, `FileStorageService` | Multipart file upload, disk storage, PDF download & preview |
| `notification` | `NotificationController`, `NotificationService` | In-app notification creation & read management |
| `dashboard` | `DashboardController`, `DashboardService` | Metrics aggregation per role |
| `admin` | `AdminController`, `AdminService` | User role alteration, user suspension, all claims query |
| `audit` | `AuditController`, `AuditService` | Immutable audit log trail per claim |
| `config` | `SecurityConfig`, `DatabaseSeeder`, `SwaggerConfig` | Security filter chain, demo seeding, OpenAPI specs |

---

## 8. Database Schema

### Key Tables

1. **`users`**: Contains account credentials, BCrypt hashes, roles, and status flags.
2. **`claims`**: Core entity recording claim number, claim type, status, amount, policy number, foreign key to customer, and assigned agent.
3. **`claim_documents`**: Stores metadata for evidence documents:
   - `id`: Primary key
   - `claim_id`: Foreign key to `claims`
   - `file_name`: Original name uploaded by the user
   - `stored_file_name`: UUID-based filename on disk
   - `file_type`: MIME type (e.g., `application/pdf`, `image/jpeg`)
   - `file_size`: Size in bytes
   - `file_path`: Absolute storage path
   - `uploaded_by`: Foreign key to `users`
   - `created_at`: Upload timestamp
4. **`audit_logs`**: Immutable event history tracking who altered what claim status and when.
5. **`notifications`**: User alert entries for workflow actions.

---

## 9. API Endpoints Reference

### 🔐 Authentication — `AuthController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 1 | `POST` | `/api/auth/register` | Public | Register new customer account |
| 2 | `POST` | `/api/auth/login` | Public | Authenticate and return JWT token |

### 👤 User Profile — `UserController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 3 | `GET` | `/api/users/profile` | All authenticated | Get authenticated user profile |

### 📋 Claims Management — `ClaimController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 4 | `POST` | `/api/claims` | CUSTOMER | Create a new claim in DRAFT status |
| 5 | `GET` | `/api/claims` | CUSTOMER | List claims filed by current customer |
| 6 | `GET` | `/api/claims/{id}` | All authenticated | Get claim details by ID |
| 7 | `PUT` | `/api/claims/{id}` | CUSTOMER | Update a DRAFT claim |
| 8 | `DELETE` | `/api/claims/{id}` | CUSTOMER | Delete a DRAFT claim |
| 9 | `POST` | `/api/claims/{id}/submit` | CUSTOMER | Submit DRAFT claim for review |

### 🔍 Claim Search — `ClaimSearchController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 10 | `GET` | `/api/claims/search` | All authenticated | Multi-filter search (role-scoped) |

### ⚙️ Workflow Actions — `WorkflowController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 11 | `GET` | `/api/workflow/claims` | CLAIM_AGENT, INVESTIGATOR, SUPERVISOR | List claims in active work queue by status |
| 12 | `POST` | `/api/workflow/claims/{id}/review` | CLAIM_AGENT | Start review (`SUBMITTED` -> `UNDER_REVIEW`) |
| 13 | `POST` | `/api/workflow/claims/{id}/approve` | CLAIM_AGENT, SUPERVISOR | Approve claim (`UNDER_REVIEW` -> `APPROVED`) |
| 14 | `POST` | `/api/workflow/claims/{id}/reject` | CLAIM_AGENT, SUPERVISOR | Reject claim (`UNDER_REVIEW` -> `REJECTED`) |
| 15 | `POST` | `/api/workflow/claims/{id}/investigate` | CLAIM_AGENT | Flag for investigation |
| 16 | `POST` | `/api/workflow/claims/{id}/start-investigation` | INVESTIGATOR | Start investigation |
| 17 | `POST` | `/api/workflow/claims/{id}/complete-investigation` | INVESTIGATOR | Complete investigation |
| 18 | `POST` | `/api/workflow/claims/{id}/close` | CLAIM_AGENT, SUPERVISOR | Close completed claim |

### 📎 Documents & PDFs — `DocumentController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 19 | `POST` | `/api/claims/{claimId}/documents` | CUSTOMER | Upload PDF/JPG/PNG evidence (max 10 MB) |
| 20 | `GET` | `/api/claims/{claimId}/documents` | **CUSTOMER, CLAIM_AGENT, INVESTIGATOR, SUPERVISOR, ADMIN** | List all documents for a claim |
| 21 | `GET` | `/api/documents/{id}` | **CUSTOMER, CLAIM_AGENT, INVESTIGATOR, SUPERVISOR, ADMIN** | Download or preview document file |
| 22 | `DELETE` | `/api/documents/{id}` | CUSTOMER, ADMIN | Delete evidence file |

### 🔔 Notifications — `NotificationController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 23 | `GET` | `/api/notifications` | All authenticated | List user notifications |
| 24 | `GET` | `/api/notifications/unread-count` | All authenticated | Get unread notification count |
| 25 | `PUT` | `/api/notifications/{id}/read` | All authenticated | Mark notification as read |

### 📊 Dashboards — `DashboardController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 26 | `GET` | `/api/dashboard/customer` | CUSTOMER | Customer statistics dashboard |
| 27 | `GET` | `/api/dashboard/agent` | CLAIM_AGENT | Claim agent workload dashboard |
| 28 | `GET` | `/api/dashboard/supervisor` | SUPERVISOR | Supervisor team overview dashboard |

### 📜 Audit Timeline — `AuditController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 29 | `GET` | `/api/claims/{claimId}/timeline` | All authenticated | Activity timeline of claim events |

### 🛡️ Admin Management — `AdminController`
| # | Method | Endpoint | Roles Allowed | Description |
|---|---|---|:---:|---|
| 30 | `GET` | `/api/admin/users` | ADMIN | List all system users (paginated) |
| 31 | `GET` | `/api/admin/users/{id}` | ADMIN | Retrieve user details |
| 32 | `PUT` | `/api/admin/users/{id}/role` | ADMIN | Change user role |
| 33 | `PUT` | `/api/admin/users/{id}/toggle-status` | ADMIN | Enable or disable user |
| 34 | `GET` | `/api/admin/claims` | ADMIN | List all claims across the system |
| 35 | `GET` | `/api/admin/claims/{id}` | ADMIN | Inspect any claim details |
| 36 | `GET` | `/api/admin/dashboard` | ADMIN | System-wide statistics |

---

## 10. Claim Lifecycle & Statuses

```
[ DRAFT ]
    |
    v (submit)
[ SUBMITTED ] ---------> [ UNDER_REVIEW ] (Claim Agent)
                             |          |
      +----------------------+          +----------------------+
      | (Approve/Reject)                                       | (Investigate)
      v                                                        v
[ APPROVED / REJECTED ]                             [ INVESTIGATION_REQUIRED ]
      |                                                        |
      |                                                        v (Start)
      |                                             [ UNDER_INVESTIGATION ]
      |                                                        |
      |                                                        v (Complete)
      |                                             [ INVESTIGATION_COMPLETED ]
      |                                                        |
      +--------------------------------------------------------+
                                  |
                                  v
                               [ CLOSED ]
```

---

## 11. Claim Types
- `HEALTH` (Medical treatments, hospitalization)
- `AUTO` (Collision, vehicle theft, repairs)
- `HOME` (Basement flood, roof leaks, structural damage)
- `LIFE` (Life insurance, critical illness)
- `TRAVEL` (Trip cancellation, lost luggage, emergency abroad)
- `PROPERTY` (Warehouse storage, fire, burglary)
- `LIABILITY` (Third-party injury, property damage liability)

---

## 12. Frontend Structure

```
frontend/src/
+-- api/              # Axios instance & global interceptors
+-- components/       # UI building blocks (Pagination, StatusChip, etc.)
+-- contexts/         # AuthContext (session, login, token management)
+-- layouts/          # AppLayout & navigation sidebar
+-- pages/
|   +-- auth/         # LoginPage (2-stage alerts & dummy mode), RegisterPage
|   +-- AdminPages.tsx       # UsersPage, AdminClaimsPage (with inline PDF viewer)
|   +-- ClaimsPage.tsx       # Customer claims list
|   +-- ClaimDetailPage.tsx  # Customer claim review & document upload
|   +-- WorkQueuePage.tsx    # Staff work queue (with inline PDF viewer)
|   +-- DashboardPage.tsx    # Role-adaptive dashboards
+-- services/         # API service wrappers (claimService, documentService, etc.)
```

---

## 13. Security & Authentication

### 2-Stage Login Alert Sequence
1. The user selects a role from the dropdown (`CUSTOMER`, `CLAIM_AGENT`, `INVESTIGATOR`, `SUPERVISOR`, `ADMIN`) and enters credentials.
2. If credentials are correct and match the DB role: The user is redirected to their genuine dashboard.
3. If credentials fail or the user is not found:
   - **Alert 1:** A modal appears displaying *"Invalid credentials -> you are not authorized."* with an automated 3-second countdown.
   - **Alert 2:** Exactly when 3 seconds elapse, Alert 1 dismisses and Alert 2 displays *"You can try dummy [role] mode with these credentials and password:"* with email, password, copy buttons, and an **Autofill Dummy Credentials** action button.

---

## 14. Configuration Reference

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/icps_db
    username: postgres
    password: 12345
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D635166546A576E5A7234753778214125442A47
  expiration: 86400000 # 24 hours

file:
  upload-dir: ./uploads
```

---

## 15. Running the Project

### Start Backend
```powershell
mvn spring-boot:run
```
Running on `http://localhost:8080` (Swagger UI at `/swagger-ui.html`).

### Start Frontend
```powershell
cd frontend
npm run dev
```
Running on `http://localhost:5173`.

---

## 16. Known Issues, Troubleshooting & Bug Resolutions

### Issue: PDF Uploaded by Customer Not Viewable or Downloadable by Staff and Admin

#### Problem Description
A critical functional defect occurred during claim processing:
Customers were able to successfully upload supporting evidence files (such as medical invoices, vehicle damage photos, and police incident reports in PDF format) to their claims via `POST /api/claims/{claimId}/documents`. The files were saved to disk and recorded in the `claim_documents` database table.

However:
- When the assigned **Claim Agent** opened the claim in their Work Queue to review evidence, the documents were inaccessible or could not be downloaded.
- When an **Investigator** or **Supervisor** was assigned to review the claim, they could not view or download the uploaded PDFs.
- The **Admin** could see claims listed on `/admin/claims`, but had no mechanism to inspect or download the customer evidence PDFs across claims.
- Any direct attempt by staff or admin accounts to download or list documents via API returned `403 Forbidden` (`Access Denied`).

---

#### Technical Root Cause Analysis (RCA)

Investigation revealed three distinct underlying causes spanning both the backend security layer and the frontend interface:

1. **Backend Controller `@PreAuthorize` Security Annotation Mismatch:**
   - In `DocumentController.java`, the endpoints responsible for listing and downloading documents were annotated as:
     ```java
     // DEFECTIVE IMPLEMENTATION:
     @GetMapping("/api/claims/{claimId}/documents")
     @PreAuthorize("hasRole('CUSTOMER')")
     public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByClaimId(...) { ... }

     @GetMapping("/api/documents/{id}")
     @PreAuthorize("hasRole('CUSTOMER')")
     public ResponseEntity<Resource> downloadDocument(...) { ... }
     ```
   - Because Spring Security evaluates `@PreAuthorize` before invoking controller methods, any request bearing a JWT token with roles `CLAIM_AGENT`, `INVESTIGATOR`, `SUPERVISOR`, or `ADMIN` was intercepted and rejected with `403 Forbidden`.
   - Even though the underlying service layer (`DocumentService.java`) possessed logic intended to grant access to privileged roles (`validateClaimAccess`), execution never reached the service because the controller blocked it at the HTTP gate.

2. **Frontend Work Queue Document Panel Absence:**
   - In `WorkQueuePage.tsx`, staff had buttons to progress workflow steps (`Start review`, `Approve`, `Reject`, `Investigate`), but the table lacked an integrated document viewer component. Staff could see claim metadata (amount, customer name, date), but had no UI button to fetch or download the evidence files.

3. **Frontend Admin Claims Page Missing Document Management:**
   - In `AdminPages.tsx` (`AdminClaimsPage`), the table rendered claims in read-only form without any column or accordion panel for attached documents. The Admin role was unable to oversee or download customer evidence.

4. **Transient Local Build State:**
   - Previous modifications to Java source files were compiled but the running Spring Boot daemon had not reloaded the updated bytecode into memory.

---

#### Step-by-Step Resolution Implemented

To resolve the defect across the entire stack, the following architectural fixes were applied:

##### 1. Backend Controller Authorization Fix (`DocumentController.java`)
The `@PreAuthorize` expressions were expanded to explicitly authorize all authenticated roles required for claim processing:
```java
// RESOLVED IMPLEMENTATION:
@GetMapping("/api/claims/{claimId}/documents")
@PreAuthorize("hasAnyRole('CUSTOMER','CLAIM_AGENT','INVESTIGATOR','SUPERVISOR','ADMIN')")
public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByClaimId(
        @PathVariable Long claimId,
        Authentication authentication
) {
    List<DocumentResponse> documents = documentService.getDocumentsByClaimId(claimId, authentication.getName());
    return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
}

@GetMapping("/api/documents/{id}")
@PreAuthorize("hasAnyRole('CUSTOMER','CLAIM_AGENT','INVESTIGATOR','SUPERVISOR','ADMIN')")
public ResponseEntity<Resource> downloadDocument(
        @PathVariable Long id,
        Authentication authentication
) {
    ClaimDocument document = documentService.getDocumentEntity(id);
    Resource resource = documentService.downloadDocument(id, authentication.getName());

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(document.getFileType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
            .body(resource);
}
```

##### 2. Service Layer Access Validation (`DocumentService.java`)
The service layer enforces business-level authorization rules:
- **Customer:** Permitted to access documents only on claims they personally created (`claim.getCustomer().getId().equals(user.getId())`).
- **Claim Agent:** Permitted to access documents on assigned claims or claims in active review.
- **Supervisor & Investigator:** Permitted privileged access to evaluate claim evidence.
- **Admin:** Granted global oversight access to all claim evidence across the system.

##### 3. Work Queue Inline Document Panel (`WorkQueuePage.tsx`)
Added `ClaimDocumentsPanel` with an accordion toggle (`📎 Documents`) on every row in the Work Queue:
- Lists all customer-uploaded documents for the selected claim.
- **View Button:** Opens the PDF directly in a new browser tab using `documentService.view(doc.id)` (`URL.createObjectURL` with `application/pdf` MIME type).
- **Download Button:** Downloads the original file to the user's computer via `documentService.download(doc.id, fileName)`.

##### 4. Admin All Claims Document Panel (`AdminPages.tsx`)
Added `AdminClaimDocumentsPanel` with an inline **"📎 View PDFs"** toggle on `/admin/claims`:
- Admin can expand any claim and view or download any customer PDF.
- Displays upload date, file size, and original filename.

##### 5. Sample PDF Evidence Seeding (`DatabaseSeeder.java`)
Added automated generation of a compliant single-page PDF (`./uploads/sample_claim_evidence.pdf`) and linked sample `ClaimDocument` records to seeded customer claims, ensuring immediate document availability during demonstrations and test runs.

---

#### Verification Scenarios & Validation

| Verification Scenario | Action | Expected Result | Status |
|---|---|---|:---:|
| **Customer Upload** | Log in as Customer (`dummy_customer@gmail.com`), upload PDF to draft claim. | Document uploaded successfully, saved to `./uploads/`, visible in claim details. | ✅ Verified |
| **Claim Agent Work Queue** | Log in as Claim Agent (`dummy_claimagent@gmail.com`), open Work Queue, click "Documents". | Document list loads without 403 error. Clicking "View" opens PDF in browser tab; clicking "Download" downloads file. | ✅ Verified |
| **Supervisor Review** | Log in as Supervisor (`dummy_supervisor@gmail.com`), inspect claim documents. | Customer evidence PDF is viewable and downloadable. | ✅ Verified |
| **Investigator Review** | Log in as Investigator (`dummy_investigator@gmail.com`), inspect claim documents. | Customer evidence PDF is viewable and downloadable. | ✅ Verified |
| **Admin System-Wide Access** | Log in as Admin (`dummy_admin@gmail.com` or `admin@icps.com`), navigate to `/admin/claims`, click "View PDFs". | Full document list for any claim is displayed; files can be viewed and downloaded without restriction. | ✅ Verified |
