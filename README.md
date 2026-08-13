# Social Event Manager

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=social-event-manager-api&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=social-event-manager-api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=social-event-manager-api&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=social-event-manager-api)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=social-event-manager-api&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=social-event-manager-api)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=social-event-manager-api&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=social-event-manager-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=social-event-manager-api&metric=coverage)](https://sonarcloud.io/summary/new_code?id=social-event-manager-api)

Full-stack web application for creating and managing social events, inviting participants, and tracking shared contributions.

**Live demo:** [social-event-manager-web.vercel.app](https://social-event-manager-web.vercel.app)

---

## Stack

**Frontend**

- Angular 22 with standalone components and Signals
- Tailwind CSS
- ngx-translate (i18n — English / Spanish)
- Deployed on Vercel

**Backend**

- Java 17 + Spring Boot 3.5
- Spring Security with JWT + OAuth2 (Google, GitHub)
- Apache Kafka (event-driven architecture)
- PostgreSQL 16 with Flyway migrations
- Deployed on Render

**Infrastructure**

- PostgreSQL on Neon
- Kafka on Aiven
- Email delivery via Resend
- Code quality via SonarCloud
- Dependency vulnerability scanning via OWASP Dependency Check

---

## Architecture

The backend follows a layered architecture with an event-driven approach for async operations:

```
Frontend (Angular) ──► REST API (Spring Boot)
                              │
                    ┌─────────┴──────────┐
                    │                    │
               PostgreSQL            Kafka Topics
               (Neon)                    │
                              ┌──────────┴──────────┐
                              │                     │
                         Email Service          Notification
                         (Resend)               Service (SSE)
```

**Kafka topics:**

- `invitations` — invitation created / responded events
- `events` — event cancelled / reminder events
- `users` — user registered / password reset events
- `notifications` — in-app notification events

---

## Features

**Events**

- Create, edit and cancel events with Google Maps location autocomplete
- Event calendar view
- Dashboard with stats and recent activity

**Invitations**

- Invite registered users or external users by email
- External users receive a unique link to join
- Accept / reject invitations
- Real-time notifications via SSE

**Contributions**

- Track what each participant brings to the event
- Optional cost splitting
- Balance calculator to settle debts between participants

**Auth**

- Email / password registration and login
- Social login with Google and GitHub
- JWT with refresh token rotation
- Password recovery via email
- Change or set password from the header

**Notifications**

- Real-time in-app notifications via Server-Sent Events (SSE)
- Notification bell with unread badge
- Full notification history with pagination
- Mark as read / mark all as read

**Emails**

- Welcome, invitation, event cancelled, event reminder, password reset
- All emails in English and Spanish based on user language

**i18n**

- Full English / Spanish support
- Language selector in header
- Browser language detection on first visit
- Persisted in localStorage

---

## Security

- JWT authentication with access + refresh tokens
- OAuth2 social login (Google, GitHub)
- OWASP Dependency Check integrated in CI — build fails on CVSS ≥ 7
- SonarCloud quality gate: Security A, Reliability A, Maintainability A
- Password validation (min 8 chars, uppercase, lowercase, number)
- Email validation with custom validator
- SQL injection protection via Spring Data JPA (prepared statements)
- CORS configured per environment

---

## Running Locally

### Prerequisites

- Java 17+
- Node.js 22+
- Maven 3.9+
- PostgreSQL (or use Neon free tier)
- Kafka (or use Aiven free tier)

### Backend

1. Clone the repository:

```bash
git clone https://github.com/facundojoaquintorres8/social-event-manager-api
cd social-event-manager-api
```

2. Create `src/main/resources/application-local.yml` with your environment variables (see [Environment Variables](#environment-variables) below).

3. Run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend

1. Clone the repository:

```bash
git clone https://github.com/facundojoaquintorres8/social-event-manager-web
cd social-event-manager-web
```

2. Install dependencies:

```bash
npm install
```

3. Run:

```bash
ng serve
```

---

## Testing

### Unit tests (backend)

```bash
mvn test
```

81 unit tests covering all service layer classes: `AuthService`, `EventService`, `InvitationService`, `ContributionService`, `ExternalInvitationService`, `NotificationService`, `NotificationLogService`, `UserService`.

### Code coverage

```bash
mvn verify
```

JaCoCo report is generated at `target/site/jacoco/index.html`.

### Dependency vulnerability check

```bash
mvn dependency-check:check -DNVD_API_KEY=your-api-key
```

---

## Environment Variables

### Backend (`application-local.yml`)

| Variable                    | Description                                    |
| --------------------------- | ---------------------------------------------- |
| `DB_URL`                    | PostgreSQL connection URL                      |
| `DB_USER`                   | Database username                              |
| `DB_PASSWORD`               | Database password                              |
| `JWT_SECRET`                | Secret key for JWT signing (min 256 bits)      |
| `KAFKA_BOOTSTRAP_SERVER`    | Kafka broker URL                               |
| `KAFKA_USERNAME`            | Kafka username                                 |
| `KAFKA_PASSWORD`            | Kafka password                                 |
| `KAFKA_TRUSTSTORE_PASSWORD` | JKS truststore password                        |
| `RESEND_API_KEY`            | Resend API key for email delivery              |
| `RESEND_FROM_EMAIL`         | Sender email address                           |
| `RESEND_OVERRIDE_TO`        | Override all emails to this address (dev only) |
| `FRONTEND_URL`              | Frontend base URL for redirects                |
| `CORS_ALLOWED_ORIGINS`      | Allowed CORS origins                           |
| `GOOGLE_CLIENT_ID`          | Google OAuth2 client ID                        |
| `GOOGLE_CLIENT_SECRET`      | Google OAuth2 client secret                    |
| `GITHUB_CLIENT_ID`          | GitHub OAuth2 client ID                        |
| `GITHUB_CLIENT_SECRET`      | GitHub OAuth2 client secret                    |

### Frontend (`environment.ts`)

| Variable | Description          |
| -------- | -------------------- |
| `apiUrl` | Backend API base URL |

---

## Author

Facundo J. Torres — [facundojoaquintorres8@gmail.com](mailto:facundojoaquintorres8@gmail.com)
