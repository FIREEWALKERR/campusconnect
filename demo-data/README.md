# Complete fictional placement dataset

This folder contains the Java dataset generator used to populate the demonstrated installation. It recreates the same fictional identities and recruitment scenarios in MySQL, with fresh dates, generated IDs, resume filenames and a newly generated BCrypt-protected demo password. It is not a backup of personal records or a SQL snapshot.

Included: 36 students with academic profiles and DOCX resumes; 8 companies/recruiters (approved, pending and rejected); 2 coordinators; 6 departments; 12 skills; 3 drives; 24 jobs (12 published, 6 closed, 6 draft); 98 applications; 400 history events; 49 interviews; 29 offers; 6 announcements; 249 notifications; and an import audit entry. Existing departments/skills are reused.

## Import on Windows

1. Extract the full ZIP. Configure MySQL and `.env` using the main README. Use the `bootstrap` profile with your own administrator email/password. Do not use the small built-in `dev` dataset at the same time.
2. Start the application once to create its tables and administrator. Stop the application before importing into a new installation.
3. From the extracted `campusconnect` folder, run with a JDK 21 installation:

```powershell
powershell -ExecutionPolicy Bypass -File .\demo-data\Import-DemoData.ps1 -JavaHome "C:\path\to\jdk-21"
```

The importer reads database credentials and resume storage location from your `.env`. Keep MySQL running. It uses the dependency JARs in the bundled application, so Maven and dependency downloads are unnecessary for importing.

4. Open `demo-data/GENERATED-ACCOUNTS.md` for the newly generated demo password and complete account list. Your administrator login is unchanged.
5. Restart the application normally, then sign in at http://localhost:8080 (or your configured port).

Student logins follow `student01@demo.campusconnect.test` through `student36@demo.campusconnect.test`; recruiters use `recruiter01` through `recruiter08` at the same domain; coordinators use `coordinator1` and `coordinator2`.

The importer preserves existing records and executes database changes in one transaction. A marker prevents importing this dataset twice. Conflicting sample emails cause a rollback instead of overwriting accounts. Never remove the marker to force a second import. On the already populated original installation, rerunning reports that the dataset exists; use its existing DUMMY-DATA.md login guide.

All records are fictional development fixtures. They are persisted in database tables and used by real application queries; live dashboards do not contain fixed demo totals. No local database credentials, personal records, or existing account passwords are included in this ZIP.
