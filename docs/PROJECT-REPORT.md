# College project report outline

## 1. Title and abstract

**CampusConnect: College Placement Management System**

CampusConnect is a Java-based web application that centralizes college recruitment activities. It connects students, recruiters, placement officers and coordinators through secure role-based access. Students maintain academic profiles, discover eligible jobs, track applications, attend interviews and respond to offers. Recruiters publish openings after approval and manage company-specific applicants. Placement staff coordinate drives and measure outcomes using database-backed reports. Spring Boot, Spring Security, Thymeleaf, JPA and MySQL provide a maintainable layered implementation with validation, transactions and auditable workflows.

## 2. Problem statement

Manual spreadsheets, email chains and disconnected forms make it difficult to enforce eligibility consistently, protect student data, avoid duplicate applications and calculate reliable placement statistics. A shared system with explicit permissions and persistent workflow history addresses these problems.

## 3. Objectives

- Centralize profiles, jobs, applications, interviews and offers.
- Enforce academic eligibility at the server.
- Separate student, recruiter, coordinator and administrator responsibilities.
- Preserve recruitment history and provide timely in-app updates.
- Produce transparent reports without duplicate placement counts.
- Demonstrate Java OOP, relational modeling, transactions, MVC and automated testing.

## 4. Scope and requirements

Discuss the four roles, functional modules, responsive interface, secure uploads, availability assumptions and local MySQL deployment. Explain that external email/SMS, SSO, multi-campus tenancy and automated aptitude assessment are outside the implemented scope.

## 5. Technology choices

Explain Java 21, Spring Boot 4.1.2, Spring MVC, Thymeleaf, Spring Security, BCrypt, JPA/Hibernate, Jakarta validation, MySQL 8, Maven, Bootstrap and JUnit 5/Mockito. Include the JUnit 5 lifecycle adaptation for Spring Framework 7 described in the README.

## 6. Analysis and design

Include role use-case diagrams, the architecture flow and ER diagram from ARCHITECTURE.md. Discuss unique constraints, foreign keys, normalized skills and eligibility mappings, immutable audit/history records, decimal currency and UTC timestamps.

## 7. Implementation modules

1. Authentication, registration, sessions and password changes.
2. Student profiles, skill mapping and protected resume storage.
3. Company approval, jobs, placement drives and academic years.
4. Reusable eligibility service.
5. Application workflow, history and company ownership.
6. Interviews, placement offers and student responses.
7. Notifications, announcements and audits.
8. Dashboards, filtered reports and CSV exports.

For each module, explain its controller, form, service, repository and entities with one real request flow. Include screenshots from your own running installation rather than fictional screen designs.

## 8. Testing and results

Document `mvn clean verify`, the test counts from `target/surefire-reports`, the installed MySQL smoke test, browser checks and edge cases. Include positive and negative tests: duplicate registration/application, deadline boundary, missing academics, unauthorized company access, missing CSRF, invalid transitions, expired offers, repeat responses and zero-student reports. Do not describe an unexecuted test as passed.

## 9. Deployment

Describe Java/Maven prerequisites, database setup, environment variables, development seed profile, first administrator bootstrap, JAR execution and file backups. Include the project run commands and demonstration accounts from the README.

## 10. Results, limitations and future enhancements

Summarize the working workflows. Discuss paginated database projections for larger cohorts, reviewed schema migrations, SSO, account recovery, automated email, resume malware scanning, interview calendar integration, accessibility review and centralized monitoring as future work.

## 11. Viva preparation

- **Why services?** They centralize eligibility, permissions and transactional changes independently of HTML controllers.
- **Why unique application constraints?** An application-level check alone cannot prevent two simultaneous submissions.
- **Why BCrypt?** It stores salted adaptive hashes rather than recoverable passwords.
- **What is CSRF?** An unwanted authenticated browser request; token validation protects mutation forms.
- **How are recruiters isolated?** The service compares the authenticated user with the job company's owner on every sensitive action.
- **Why BigDecimal?** Financial values require decimal arithmetic without binary floating-point rounding artifacts.
- **Why a history table?** Current state answers “where now”; history answers “how and when did it change?”.
- **How is placement percentage calculated?** Distinct accepted-offer students in the eligible reporting cohort divided by that cohort, with a zero-denominator guard.
- **How does optimistic locking help?** Entity versions detect concurrent updates rather than silently losing one user's changes.
- **Why is open-in-view disabled?** Data requirements are resolved inside transactions; view rendering does not silently issue database queries.
- **Why do tests use H2?** Fast isolated tests without a shared server; real MySQL still needs deployment verification.
- **What happens without seed data?** Public pages show honest empty states; registration and explicit admin bootstrap remain functional.

## 12. References and appendices

Reference official Java, Spring, Hibernate, MySQL, Thymeleaf, Bootstrap and JUnit documentation. Append the route matrix, ER diagram, testing evidence, selected service code and installation guide. Include original authorship acknowledgements and your institution's required report format.
