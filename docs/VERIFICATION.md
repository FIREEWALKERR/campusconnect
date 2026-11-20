# Verification record

Verified on 4 November 2026 with Java 21 and Maven 3.9.11.

## Build result

`mvn clean verify` completed successfully. Spring Boot produced an executable `campusconnect-1.0.0.jar` with runtime dependencies.

**28 tests passed; 0 failures, 0 errors, 0 skipped.**

- 10 unit tests: eligibility, deadline boundaries, unpublished/unapproved jobs, missing academics, duplicate applications, state transitions, zero-denominator/rounding and CSV formula protection.
- 17 integration tests: real application startup, database persistence, secured MVC, rendered templates, login/roles/CSRF, registration validation, full application-to-offer workflow, ownership, expired/declined offers, distinct placement statistics, CSV endpoints, resume validation/download protection, database duplicate constraints, cross-field job validation, dynamic departments and coordinator creation.
- 1 empty-installation test: no development profile, zero users, public pages and real registration.

Tests use isolated H2 databases in MySQL compatibility mode. The normal application and packaged JAR use MySQL Connector/J; H2 is test-scoped. A subsequent live smoke test ran the packaged application against MySQL 8.4.13. All 17 tables were created, administrator authentication succeeded, and the homepage, dashboard, students, departments, coordinators, companies, jobs, applications, interviews, offers, reports and audit pages returned HTTP 200. Long student narrative fields use TEXT-compatible LOB mappings to satisfy MySQL row-size limits.

The local test-profile UI was inspected at mobile-width and desktop-width layouts, and administrator login was exercised in the browser. Subsequent configuration/catalog changes were checked through the rendered-template integration tests. No production or external recruitment records were used.

## Reproduce

```shell
mvn clean verify
```

The result details are generated in `target/surefire-reports`. The optional development fixture is external to Java business logic and requires a password supplied via environment; the normal application has no default password or embedded business records.
