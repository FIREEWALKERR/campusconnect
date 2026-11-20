# Run CampusConnect on Windows

1. **Extract the ZIP** into a normal folder. Open PowerShell in the extracted `campusconnect` folder containing `pom.xml`.
2. **Install JDK 21, Maven 3.9.x and MySQL 8.x.** Check `java -version` and `mvn -version`.
3. **Create the database.** Open `docs/database-setup.sql`, replace its password placeholder with your own password, and run it in MySQL Workbench. Alternatively configure `.env` and run `docker compose up -d mysql` with Docker Desktop running.
4. **Configure your installation:**

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `BOOTSTRAP_EMAIL` and `BOOTSTRAP_PASSWORD`. Keep `SPRING_PROFILES_ACTIVE=bootstrap` for an empty installation. The administrator password must be at least 12 characters and at most 72 UTF-8 bytes. All accounts and business records are stored in MySQL; there is no default login password.

5. **Build and test:**

   ```powershell
   mvn clean verify
   ```

   Tests use an isolated test database and do not alter your MySQL database.

6. **Start:**

   ```powershell
   .\scripts\run.ps1
   ```

   Or use the packaged JAR (Java 21 required; Maven is unnecessary for this option):

   ```powershell
   .\scripts\run.ps1 -UseJar
   ```

7. Open **http://localhost:8080** (or the `SERVER_PORT` you configured). Sign in with your chosen administrator email/password. Add departments through **Departments**, register students and recruiters, approve companies, and create real openings.
8. After the first administrator has been created, stop with **Ctrl+C**, clear `SPRING_PROFILES_ACTIVE`, `BOOTSTRAP_EMAIL` and `BOOTSTRAP_PASSWORD` in `.env`, and start again. Existing database accounts remain usable.

## Optional demonstration data

Use a separate empty database, set `SPRING_PROFILES_ACTIVE=dev` and supply your own `DEMO_PASSWORD`. The application imports `src/main/resources/demo-data.properties` only in this profile and only when there are no users. The file contains editable fictional examples, not production business logic. Override its path with `DEMO_DATA_LOCATION=file:/absolute/path/to/dataset.properties` if desired.

The bundled sample emails are `admin@campusconnect.test`, `coordinator@campusconnect.test`, `student@campusconnect.test` and `recruiter@campusconnect.test`. Their password is the value **you** set in `DEMO_PASSWORD`; there is no built-in password.

## Direct Maven / Java commands

The helper reads `.env` without executing its contents. If you prefer not to use it, export the same variables in your shell or IDE and run:

```shell
mvn test
mvn clean package
mvn spring-boot:run
java -jar target/campusconnect-1.0.0.jar
```

Choose either `spring-boot:run` or `java -jar` to start one instance. `.env` is not automatically read by those direct commands. See README.md for architecture, configuration, testing and troubleshooting.
