# Configuration and data ownership

Normal operation has no default login password and no embedded students, recruiters, jobs, offers or statistics. The `bootstrap` profile creates a single administrator using environment values. The optional `dev` profile imports an explicitly selected external dataset into an empty database.

| Item | Where it comes from |
|---|---|
| Database host, schema, username, password | Required DB_URL, DB_USERNAME, DB_PASSWORD |
| First administrator identity/password | BOOTSTRAP_EMAIL, BOOTSTRAP_PASSWORD |
| Department catalog | Admin → Departments; stored in MySQL |
| Graduation batches | Student/profile and job forms; no fixed year menu |
| Academic years and drives | Placement-drive management |
| Company, job, salary, criteria, vacancies, deadlines | Validated database-backed forms |
| Applications, interview rounds and offers | Transactional recruitment services |
| Dashboard totals and placement percentage | Computed from database records |
| Salary distribution bounds | Derived from actual minimum and maximum offered salaries |
| Distribution bucket count | SALARY_BUCKETS (default 4, limited to 1–20 for display) |
| Entry time zone | APP_TIME_ZONE (default Asia/Kolkata); validation uses the same zone |
| Currency label | CURRENCY_CODE (default INR); this labels amounts, it does not perform exchange conversion |
| College contact | PLACEMENT_CONTACT_EMAIL; omitted when not configured |
| Resume directory and size | RESUME_DIRECTORY, RESUME_MAX_SIZE, REQUEST_MAX_SIZE |
| Session timeout / secure cookie | SESSION_TIMEOUT, COOKIE_SECURE |
| HTTP port | SERVER_PORT |
| Optional demo data location | DEMO_DATA_LOCATION; defaults to the separately editable classpath sample file |
| Demo password | Required DEMO_PASSWORD; never stored in the dataset |

The source still defines the application's required role/state enums, validation rules, security policy, mathematical formulas, field labels, routes and visual styling. These are program definitions, not substituted business data. Test-only fixtures deliberately use known inputs to verify outcomes and are excluded from the executable application's runtime classpath.

Changing currency labels does not convert existing salaries. Choose the institutional currency before entering records. Time is persisted in UTC; configured local input times are converted to UTC. Keep the database and resume directory backed up together.

The fictional sample dataset may be edited without recompiling Java when `DEMO_DATA_LOCATION` points to an external properties file. The importer never runs in a normal or bootstrap profile and never replaces an existing user database.
