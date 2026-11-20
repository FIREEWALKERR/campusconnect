# CampusConnect — College Placement Management System

A Java 21 / Spring Boot 4.1.2 application with server-rendered Thymeleaf pages, Spring Security, MySQL persistence and four roles. All recruitment decisions and statistics run in Java against persisted records. There is no Node.js runtime or separate frontend backend.

Start with **[RUN-STEPS.md](RUN-STEPS.md)** for the Windows setup and Desktop ZIP instructions. A normal installation starts empty and creates only the administrator you configure.

## Prerequisites

- JDK 21, with `JAVA_HOME` pointing at the JDK and `java` on PATH.
- Maven 3.9.x, with `mvn` on PATH.
- MySQL 8.x (8.4 LTS recommended), or Docker Desktop for the optional database container.
- An internet connection for the first Maven dependency download. Bootstrap and Bootstrap Icons are bundled as WebJars; browser CDN access is unnecessary.

Open this folder's `pom.xml` in IntelliJ IDEA, Eclipse with m2e, or VS Code with the Java extension pack. Select JDK 21. Lombok supplies accessors; enable annotation processing in IDEs that require it. Business logic is ordinary Java in the service package.

## 1. Prepare MySQL

Run the statements in `docs/database-setup.sql` in MySQL Workbench or the MySQL CLI after replacing its example password. The application uses database `campusconnect` and user `campusconnect`.

Alternatively, copy `.env.example` to `.env`, set both `DB_PASSWORD` and `DB_ROOT_PASSWORD`, and run:

```shell
docker compose up -d mysql
docker compose ps
```

The Compose volume preserves database data. Do not remove it unless you deliberately want to erase that database. `.env` is consumed by Docker Compose; Spring Boot does **not** automatically read this file. Export the application variables below separately.

## 2. Configure and run a demonstration

PowerShell, from this directory:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/campusconnect?connectionTimeZone=UTC'
$env:DB_USERNAME = 'campusconnect'
$env:DB_PASSWORD = 'your-database-password'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:DEMO_PASSWORD = 'choose-your-own-demo-password'
mvn clean verify
mvn spring-boot:run
```

macOS / Linux:

```bash
export DB_URL='jdbc:mysql://localhost:3306/campusconnect?connectionTimeZone=UTC'
export DB_USERNAME='campusconnect'
export DB_PASSWORD='your-database-password'
export SPRING_PROFILES_ACTIVE=dev
export DEMO_PASSWORD='choose-your-own-demo-password'
mvn clean verify
mvn spring-boot:run
```

Open **http://localhost:8080**. Maven tests use an isolated H2 database in MySQL compatibility mode and do not need a MySQL installation. Running the application normally always uses MySQL.

Exact standalone commands:

```shell
mvn test
mvn clean package
java -jar target/campusconnect-1.0.0.jar
```

Environment variables also apply when running the JAR. Use `SERVER_PORT` to change port 8080.

## Development accounts

These are created **only with the `dev` profile**, and only when the user table is empty. The initializer is transactional and never replaces an existing installation.

| Role | Email | Password supplied by you |
|---|---|---|
| Administrator | admin@campusconnect.test | Your DEMO_PASSWORD |
| Coordinator | coordinator@campusconnect.test | Your DEMO_PASSWORD |
| Student | student@campusconnect.test | Your DEMO_PASSWORD |
| Recruiter | recruiter@campusconnect.test | Your DEMO_PASSWORD |

Additional students: `student2@campusconnect.test` through `student8@campusconnect.test`. Additional recruiters: `recruiter2@campusconnect.test` through `recruiter4@campusconnect.test`. The fourth recruiter awaits approval. Company names and people are fictional demonstration records.

You must set `DEMO_PASSWORD` before the first development run. No default password is provided. Example records are imported from the external `demo-data.properties` file; override it with `DEMO_DATA_LOCATION`. Passwords are encoded using the configured BCrypt encoder. Changing `DEMO_PASSWORD` later does not reset existing accounts. Use Account settings to change a password.

## Empty installation without demonstration records

Public pages and registration work without seed records. For a fresh administrator account, use the explicit one-time `bootstrap` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'bootstrap'
$env:BOOTSTRAP_EMAIL = 'placement@your-college.edu'
$env:BOOTSTRAP_PASSWORD = 'choose-a-strong-unique-password'
mvn spring-boot:run
```

After initialization, remove `SPRING_PROFILES_ACTIVE`, `BOOTSTRAP_EMAIL` and `BOOTSTRAP_PASSWORD` from the deployment environment and restart. Bootstrap does not run under `dev` and never overwrites an existing administrator. New students and recruiters register through the public forms. Administrators can create coordinator accounts under Coordinators; the development profile also includes a coordinator example. Departments are administrator-managed database records; add them before students complete their profiles.

## What is implemented

- Four role-specific navigation menus and scoped dashboards, database authentication, BCrypt, CSRF, session fixation protection, POST logout and password changes.
- Student registration, unique email and registration number, academic/personal profile, reusable skills, projects, certifications, achievements, profile completion and protected PDF/DOCX resume uploads.
- Recruiter registration, profile editing and administrator approval. Unapproved recruiters may prepare drafts but cannot publish.
- Job creation/editing, draft/published/closed/cancelled states, safe unused-draft deletion, normalized eligible departments and batches, salary/deadline validation, search, location/salary/department/eligibility filters, database pagination and sorting.
- Server-enforced eligibility with explanations, one application per student/job, timestamped history, explicit valid status transitions, withdrawal and company ownership restrictions.
- Interview rounds, future-date validation, instructions, completion/cancellation and student visibility. To change a scheduled round, cancel it and create its replacement with the same round number.
- Offers with annual CTC, issuance/acceptance deadlines and owner-only accept/decline. Issued terms are intentionally immutable; final responses cannot be changed.
- Database notifications with unread markers, scheduled publication dates for announcements, placement-drive/academic-year management and administrator audit history.
- Student search/filter/sort/edit/disable, company approval queue, scoped applicant review, department/batch/company/academic-year reports, live placement rates and CSV exports with spreadsheet-formula neutralization.
- Responsive blue/violet interface, shared header/sidebar/footer/notification fragments, empty states, form validation, confirmations and safe error views.

See `docs/ARCHITECTURE.md` for entities, permissions, workflows and the ER diagram. See `docs/PROJECT-REPORT.md` for a college report outline and viva guide. `docs/DEMO-GUIDE.md` provides an end-to-end presentation script.

## Reporting definitions

The eligible reporting cohort is enabled students with a department, graduation year, CGPA and backlog count, optionally restricted to a graduating batch. This is deliberately separate from job-specific eligibility. Placed students are distinct members of that cohort with at least one accepted offer. Placement percentage is `placed / eligible * 100`, rounded to two decimal places; zero eligible students returns 0%.

Company and academic-year filters constrain offers, not the cohort denominator. Academic year comes from the associated placement drive. Salary summaries include all issued offers in the filtered set, including declined/pending offers; zero indicates no available salary records. Company recruitment totals are labeled all-time.

The normal application contains no embedded business records or default credentials. Departments, eligibility, deadlines, batches, salaries and outcomes are database data. Deployment settings are externalized; see `docs/CONFIGURATION.md`. Optional fictional sample records and isolated test fixtures are clearly separated.

## Storage and configuration

| Variable | Purpose |
|---|---|
| DB_URL | MySQL JDBC URL; required; set your own MySQL host/database URL |
| DB_USERNAME / DB_PASSWORD | Database account; password has no application default |
| SPRING_PROFILES_ACTIVE | `dev` for demo data; omit for normal use |
| DEMO_PASSWORD | Development seed password override |
| RESUME_DIRECTORY | Filesystem directory, defaults to `./data/resumes` |
| COOKIE_SECURE | Set `true` when serving through HTTPS |
| SERVER_PORT | HTTP port, defaults to 8080 |

Uploads use generated UUID filenames, PDF signatures / DOCX ZIP structure checks, extension and MIME validation, a configurable size limit (5 MB by default) and restricted download endpoints. DOCX decompression is bounded. Resume files are outside public assets and served as attachments with no-store headers. Back up the database and resume directory together. File signature validation is not antivirus scanning.

`ddl-auto=update` is convenient for this college project and creates an empty schema from JPA. Before a real institutional deployment, adopt reviewed Flyway/Liquibase migrations, HTTPS, backup/restore procedures, malware scanning and operational monitoring. Demo accounts must not be enabled for that deployment.

See `docs/VERIFICATION.md` for the completed build and test results.

## Tests and version compatibility

JUnit **5.14.4** is explicitly retained as requested. Spring Boot 4.1.2 normally manages JUnit 6, and Spring Framework 7's `SpringExtension` uses JUnit 6 APIs. Integration tests therefore start a real Spring Boot application with `SpringApplicationBuilder`, autowire test dependencies, construct secured MockMvc against its web context, and explicitly roll back transactional test cases. They do not use the incompatible `@SpringBootTest` extension. Mockito uses the subclass mock maker, avoiding runtime Java-agent attachment.

Tests cover registration and duplicates, authentication/roles/CSRF, academic and deadline eligibility, duplicate applications, ownership, workflow transitions, full recruitment lifecycle, offer expiry and responses, distinct placement counts, CSV escaping and rendered pages. Tests render outside a transaction where appropriate to catch lazy-loading errors. H2 tests are not a substitute for a MySQL deployment smoke test.

## Troubleshooting

- **Java not found / invalid target release:** install JDK 21 and check `java -version`, `mvn -version` and `JAVA_HOME`.
- **Database connection refused:** start MySQL, verify port 3306 and DB_URL. Docker Desktop must be running before Compose.
- **Access denied for MySQL user:** match DB_PASSWORD with the account created in MySQL; changing `.env` does not change a password in an existing Docker volume.
- **Unresolved DB_PASSWORD:** export it into the shell/IDE run configuration. `.env` is not loaded by Spring Boot.
- **No demo accounts:** `dev` must be active and the user table must have been empty at first run. Do not delete a real database to reseed it.
- **Cannot apply:** open the job detail page for the precise CGPA, department, batch, backlog, deadline or duplicate-application explanation.
- **Cannot publish a recruiter job:** an administrator must approve the company first.
- **403 on a POST:** submit the rendered form with its CSRF token; do not bypass Spring Security.
- **Missing resume after moving the app:** preserve the configured RESUME_DIRECTORY and its files.
- **Port already occupied:** set SERVER_PORT to another port.
- **Times differ from your clock:** persisted/displayed instants are UTC; date-time entry forms use APP_TIME_ZONE (default Asia/Kolkata).

Stable version references: [Spring Boot documentation](https://docs.spring.io/spring-boot/) and [JUnit 5.14.4 release notes](https://docs.junit.org/5.14.4/release-notes).

## Complete demonstration dataset

See [demo-data/README.md](demo-data/README.md) to import the full student, company, recruitment and resume dataset using Java 21.

