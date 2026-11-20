package edu.campusconnect.config;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.EventService;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports an explicit, editable external sample dataset; never supplies authentication defaults.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevelopmentData implements CommandLineRunner {
  private final UserRepository users;
  private final StudentProfileRepository students;
  private final CompanyRepository companies;
  private final JobRepository jobs;
  private final SkillRepository skills;
  private final PlacementDriveRepository drives;
  private final ApplicationRepository applications;
  private final ApplicationStatusHistoryRepository histories;
  private final InterviewRepository interviews;
  private final PlacementOfferRepository offers;
  private final AnnouncementRepository announcements;
  private final DepartmentRepository departments;
  private final PasswordEncoder encoder;
  private final EventService events;
  private final ResourceLoader resources;
  private final ZoneId applicationZone;

  @Value("${app.demo-password}")
  private String password;

  @Value("${app.demo-data-location}")
  private String location;

  private final Properties data = new Properties();

  private String value(String key) {
    return Objects.requireNonNull(data.getProperty(key), "Missing sample-data key: " + key);
  }

  private List<String> list(String key) {
    return Arrays.stream(value(key).split(",")).map(String::trim).toList();
  }

  private int number(String key) {
    return Integer.parseInt(value(key));
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    if (users.count() > 0) return;
    if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalStateException(
          "Set DEMO_PASSWORD to a password of at least 12 characters and at most 72 UTF-8 bytes"
              + " before importing sample data.");
    try (var reader =
        new InputStreamReader(
            resources.getResource(location).getInputStream(), StandardCharsets.UTF_8)) {
      data.load(reader);
    }
    int year = LocalDate.now(applicationZone).getYear();
    Instant now = Instant.now();
    Map<String, Department> ds = new LinkedHashMap<>();
    for (String key : list("departments")) {
      var d = new Department();
      d.setCode(key);
      d.setName(value("department." + key + ".name"));
      departments.save(d);
      ds.put(key, d);
    }
    Map<String, User> us = new LinkedHashMap<>();
    for (String key : list("users")) {
      String p = "user." + key + ".";
      var u = new User();
      u.setEmail(value(p + "email").toLowerCase(Locale.ROOT));
      u.setFullName(value(p + "name"));
      u.setRole(Role.valueOf(value(p + "role")));
      u.setPassword(encoder.encode(password));
      users.save(u);
      us.put(key, u);
    }
    var admin =
        us.values().stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseThrow();
    Map<String, StudentProfile> ss = new LinkedHashMap<>();
    for (String key : list("students")) {
      String p = "student." + key + ".";
      var s = new StudentProfile();
      s.setUser(us.get(key));
      s.setRegistrationNumber(value(p + "registration"));
      s.setPhone(value(p + "phone"));
      s.setDegree(value(p + "degree"));
      s.setDepartment(ds.get(value(p + "department")));
      s.setGraduationYear(year + number(p + "graduationOffset"));
      s.setCgpa(new BigDecimal(value(p + "cgpa")));
      s.setBacklogs(number(p + "backlogs"));
      s.setAcademicHistory(value(p + "academicHistory"));
      s.setProjects(value(p + "projects"));
      s.setCertifications(value(p + "certifications"));
      s.setAchievements(value(p + "achievements"));
      for (String name : list(p + "skills"))
        s.getSkills()
            .add(
                skills
                    .findByName(name)
                    .orElseGet(
                        () -> {
                          var skill = new Skill();
                          skill.setName(name);
                          return skills.save(skill);
                        }));
      students.save(s);
      ss.put(key, s);
    }
    Map<String, Company> cs = new LinkedHashMap<>();
    for (String key : list("companies")) {
      String p = "company." + key + ".";
      var c = new Company();
      c.setUser(us.get(key));
      c.setName(value(p + "name"));
      c.setDescription(value(p + "description"));
      c.setLocation(value(p + "location"));
      c.setWebsite(value(p + "website"));
      c.setApproval(ApprovalState.valueOf(value(p + "approval")));
      companies.save(c);
      cs.put(key, c);
    }
    var drive = new PlacementDrive();
    drive.setName(value("drive.name"));
    drive.setBatch(year + number("drive.batchOffset"));
    drive.setAcademicYear(year + "-" + (year + 1));
    drive.setStartDate(LocalDate.now(applicationZone));
    drive.setEndDate(drive.getStartDate().plusDays(number("drive.durationDays")));
    drive.setVenue(value("drive.venue"));
    drives.save(drive);
    Map<String, Job> js = new LinkedHashMap<>();
    for (String key : list("jobs")) {
      String p = "job." + key + ".";
      var j = new Job();
      j.setCompany(cs.get(value(p + "company")));
      j.setDrive(drive);
      j.setTitle(value(p + "title"));
      j.setDescription(value(p + "description"));
      j.setRequiredSkills(value(p + "skills"));
      j.setCategory(value(p + "category"));
      j.setLocation(value(p + "location"));
      j.setEmploymentType(EmploymentType.valueOf(value(p + "employmentType")));
      j.setSalaryMin(new BigDecimal(value(p + "salaryMin")));
      j.setSalaryMax(new BigDecimal(value(p + "salaryMax")));
      j.setMinimumCgpa(new BigDecimal(value(p + "minimumCgpa")));
      j.setMaximumBacklogs(number(p + "maximumBacklogs"));
      for (String code : list(p + "departments"))
        j.getDepartments().add(Objects.requireNonNull(ds.get(code)));
      for (String offset : list(p + "batchOffsets"))
        j.getBatches().add(year + Integer.parseInt(offset));
      j.setOpensAt(now.plus(Duration.ofDays(number(p + "opensDays"))));
      j.setDeadline(now.plus(Duration.ofDays(number(p + "deadlineDays"))));
      j.setVacancies(number(p + "vacancies"));
      j.setStatus(JobStatus.valueOf(value(p + "status")));
      jobs.save(j);
      js.put(key, j);
    }
    for (String key : list("applications")) {
      String p = "application." + key + ".";
      var path = list(p + "history").stream().map(ApplicationStatus::valueOf).toList();
      var a = new Application();
      a.setStudent(ss.get(value(p + "student")));
      a.setJob(js.get(value(p + "job")));
      a.setStatus(path.getLast());
      applications.save(a);
      ApplicationStatus previous = null;
      int step = 0;
      for (var status : path) {
        var h = new ApplicationStatusHistory();
        h.setApplication(a);
        h.setActor(admin);
        h.setPreviousStatus(previous);
        h.setStatus(status);
        h.setNote("Imported development sample history");
        h.setCreatedAt(now.minus(Duration.ofDays(path.size() - step++)));
        histories.save(h);
        previous = status;
      }
      if (path.contains(ApplicationStatus.INTERVIEW_SCHEDULED)) {
        var i = new Interview();
        i.setApplication(a);
        i.setRoundNumber(number("interview.round"));
        i.setType(value("interview.type"));
        i.setLocation(value("interview.location"));
        i.setInstructions(value("interview.instructions"));
        i.setScheduledAt(now.plus(Duration.ofDays(number(p + "interviewDays"))));
        i.setStatus(
            a.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED
                ? InterviewStatus.SCHEDULED
                : InterviewStatus.COMPLETED);
        interviews.save(i);
      }
      if (path.contains(ApplicationStatus.OFFERED)) {
        var o = new PlacementOffer();
        o.setApplication(a);
        o.setSalary(new BigDecimal(value(p + "salary")));
        o.setOfferDate(now);
        o.setAcceptanceDeadline(now.plus(Duration.ofDays(number("offer.validDays"))));
        if (a.getStatus() == ApplicationStatus.OFFER_ACCEPTED) {
          o.setStatus(OfferStatus.ACCEPTED);
          o.setRespondedAt(now);
        }
        offers.save(o);
      }
      events.notify(a.getStudent().getUser(), a.getJob().getTitle() + ": " + a.getStatus());
    }
    var a = new Announcement();
    a.setAuthor(admin);
    a.setTitle(value("announcement.title"));
    a.setBody(value("announcement.body"));
    a.setPublishedAt(now);
    announcements.save(a);
    events.audit(admin, "Imported development dataset", location);
  }
}
