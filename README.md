# Expense Claim Management System

Role-based expense claim app (Employee / Manager).

* **Backend:** Spring Boot 3.3, Java 17, Spring Security + JWT, Spring Data JPA, MySQL 8
* **Frontend:** React 18 (Vite), React Router, Axios
* **Extras:** Swagger UI, Docker Compose, CSV export

```
React (Vite) --Axios + JWT--> Spring Boot REST API --JPA--> MySQL
  route guards, role menus      JwtAuthFilter -> Controller -> Service -> Repository
  (usability only)              DTOs, @Valid, GlobalExceptionHandler, uploads/ (receipts)
```

## Quick start (local)

Requirements: Java 17+, Maven 3.9+, Node 18+, MySQL 8 running on `localhost:3306`.

```bash
# 1. Backend (creates the expense\_db database automatically)
cd backend
# edit src/main/resources/application.properties if your MySQL user/password is not root/root
mvn spring-boot:run          # http://localhost:8080

# 2. Frontend (new terminal)
cd frontend
npm install
npm run dev                  # http://localhost:5173
```

Run the unit tests: `cd backend \&\& mvn test`

## Quick start (Docker Compose)

```bash
docker compose up --build
```

* App: http://localhost:3000
* API: http://localhost:8080
* Swagger UI: http://localhost:8080/swagger-ui.html

## Sample logins

|Role|Email|Password|
|-|-|-|
|Manager|manager@example.com|Manager@123|
|Employee|register any account on `/register` (it is always created as an Employee)||

The manager account is seeded at startup (values come from `app.seed.\*` in `application.properties`).

## Requirement checklist

|#|Requirement|Where|
|-|-|-|
|3|JWT access (15 min) + refresh (7 days, stored in DB, rotated, revocable), BCrypt|`AuthService`, `JwtService`, `RefreshToken`|
|4|Register = Employee, Manager seeded|`AuthService.register`, `DataSeeder`|
|5|Employee sees only own claims (404 for others)|`ClaimService.getOwnedClaim` (`findByIdAndEmployeeId`)|
|6|Edit/delete only while PENDING|`ClaimService.requirePending` (409)|
|7|Pagination, search, status filter|`ClaimSpecifications`, `ClaimController.list`|
|8|Manager filters: status, employee, date range|`ManagerClaimController.list`|
|9|Approve/reject, comment mandatory on reject, final|`ManagerClaimService.review`|
|10|Receipt upload (JPEG/PNG/PDF, 5 MB), view/download|`FileStorageService`|
|11|Audit log + timeline|`AuditService`, `Timeline.jsx`|
|12|Dashboards|`DashboardService`|
|13|Activate/deactivate users (blocked at login, refresh and on every request)|`UserService`, `UserDetailsImpl.isEnabled`|
|14|Route guards, Access Denied, role menus|`ProtectedRoute.jsx`, `Layout.jsx`, `AccessDenied.jsx`|
|15|Axios interceptor: attach token, refresh once, logout on failure|`frontend/src/api/axios.js`|
|17-18|Layered architecture, DTOs, `@Valid`, global handler|`controller / service / repository / dto / exception`|
|19|Unit tests (16)|`backend/src/test/...`|
|20|Swagger, Docker Compose, CSV export|`/swagger-ui.html`, `docker-compose.yml`, `GET /api/manager/claims/export`|

## API list

Auth (public)

```
POST   /api/auth/register        { name, email, password }
POST   /api/auth/login           { email, password }
POST   /api/auth/refresh         { refreshToken }
POST   /api/auth/logout          { refreshToken }
```

Employee (role EMPLOYEE, except where noted)

```
GET    /api/claims?page\&size\&search\&status
POST   /api/claims
GET    /api/claims/{id}                 (employee: own only, manager: any)
PUT    /api/claims/{id}                 (pending only)
DELETE /api/claims/{id}                 (pending only, soft delete)
POST   /api/claims/{id}/receipt         (multipart field "file", pending only)
GET    /api/claims/{id}/receipt         (?download=true; owner or manager)
GET    /api/claims/{id}/audit-logs      (owner or manager)
GET    /api/dashboard/employee
```

Manager (role MANAGER)

```
GET    /api/manager/claims?status\&employeeId\&from\&to\&search\&page\&size
PATCH  /api/manager/claims/{id}/approve          { comment? }
PATCH  /api/manager/claims/{id}/reject           { comment }   (comment required)
GET    /api/manager/claims/export                 same filters, returns CSV
GET    /api/manager/users?page\&size
PATCH  /api/manager/users/{id}/status            { active: true|false }
GET    /api/dashboard/manager
```

Status codes: `400` validation / missing reject comment / bad file, `401` missing or expired token / bad login,
`403` wrong role or deactivated account, `404` unknown claim or someone else's claim, `409` business rule
(not pending, already reviewed, duplicate email), `413` file too large.

## Design notes (for the interview)

* **404 instead of 403 for another user's claim** avoids revealing that the claim exists.
* **Backend is the real protection.** Frontend guards and hidden buttons are only for usability; every endpoint is
protected by the Spring Security filter chain, `@PreAuthorize` and service-level ownership checks.
* **Deactivation takes effect immediately:** blocked at login and refresh, and `JwtAuthFilter` reloads the user on every
request, so an already-issued access token stops working too.
* **Refresh tokens** are random UUIDs stored in the database, rotated on every refresh and revoked on logout.
* **Soft delete** keeps the audit trail. Audit logs are append-only and reference the claim by id only.
* **Money** is `BigDecimal` (`DECIMAL(12,2)`), never `double`.
* **Uploads:** content type, extension and file signature (magic bytes) are all checked; files are stored under a random
UUID name, never the client's name.
* **CSV export** neutralises spreadsheet formula injection (cells starting with `= + - @`).
* Dynamic filters use JPA `Specification`s, which avoids null-parameter problems in JPQL.

## Screenshots

## Screenshots

### Login

!\[Login](docs/screenshots/01-login.png)

### Employee dashboard

!\[Employee dashboard](docs/screenshots/02-employee-dashboard.png)

### My claims (search, filter, pagination)

!\[My claims](docs/screenshots/03-my-claims.png)

### Claim details with audit timeline

!\[Claim details](docs/screenshots/04-claim-details.png)

### Manager dashboard

!\[Manager dashboard](docs/screenshots/05-manager-dashboard.png)

### Manager claims list with filters

!\[Manager claims](docs/screenshots/06-manager-claims.png)

### Users management

!\[Users](docs/screenshots/07-users.png)

### Swagger UI

!\[Swagger](docs/screenshots/08-swagger.png)

## Project structure

```
backend/   Spring Boot API (config, controller, service, repository, entity, dto, security, exception)
frontend/  React app (api, context, components, pages)
docker-compose.yml
```

