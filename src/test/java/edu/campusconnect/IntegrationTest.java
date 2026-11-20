package edu.campusconnect;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {
  @Autowired WebApplicationContext context;
  MockMvc mvc;
  @Autowired AccountService accounts;
  @Autowired ApplicationService workflow;
  @Autowired RecruitmentService recruitment;
  @Autowired ReportService reports;
  @Autowired UserRepository users;
  @Autowired StudentProfileRepository students;
  @Autowired CompanyRepository companies;
  @Autowired JobRepository jobs;
  @Autowired ApplicationRepository applications;
  @Autowired PlacementOfferRepository offers;
  @Autowired ApplicationStatusHistoryRepository histories;
  @Autowired PasswordEncoder encoder;
  org.springframework.context.ConfigurableApplicationContext running;
  org.springframework.transaction.TransactionStatus transaction;

  @BeforeAll
  void boot() {
    running =
        new org.springframework.boot.builder.SpringApplicationBuilder(
                CampusConnectApplication.class)
            .profiles("dev", "test")
            .properties("server.port=0")
            .run();
    running.getAutowireCapableBeanFactory().autowireBean(this);
  }

  @AfterAll
  void stop() {
    running.close();
  }

  @BeforeEach
  void setup(TestInfo info) {
    if (info.getTestMethod().orElseThrow().isAnnotationPresent(Transactional.class))
      transaction =
          running
              .getBean(org.springframework.transaction.PlatformTransactionManager.class)
              .getTransaction(
                  new org.springframework.transaction.support.DefaultTransactionDefinition());
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @AfterEach
  void clear() {
    if (transaction != null) {
      running
          .getBean(org.springframework.transaction.PlatformTransactionManager.class)
          .rollback(transaction);
      transaction = null;
    }
    SecurityContextHolder.clearContext();
  }

  void as(String email, Role role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                email, "ignored", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  void render(String url, String email, String role) throws Exception {
    var r =
        mvc.perform(get(url).with(user(email).roles(role))).andExpect(status().isOk()).andReturn();
    assertTrue(r.getResponse().getContentAsString().contains("CampusConnect"), url);
  }

  @Test
  void publicPagesRenderAndStaticAssetsExist() throws Exception {
    for (String url : List.of("/", "/login", "/register/student", "/register/company", "/about"))
      mvc.perform(get(url)).andExpect(status().isOk());
    mvc.perform(get("/webjars/bootstrap/5.3.8/css/bootstrap.min.css")).andExpect(status().isOk());
    mvc.perform(get("/webjars/bootstrap-icons/1.13.1/font/bootstrap-icons.css"))
        .andExpect(status().isOk());
  }

  @Test
  void studentPagesRenderOutsideTransaction() throws Exception {
    for (String url :
        List.of(
            "/dashboard",
            "/student/profile",
            "/student/profile/edit",
            "/jobs",
            "/jobs?eligibleOnly=true",
            "/applications",
            "/interviews",
            "/offers",
            "/notifications",
            "/announcements",
            "/account")) render(url, "student@campusconnect.test", "STUDENT");
    var j = jobs.findAll().getFirst();
    render("/jobs/" + j.getId(), "student@campusconnect.test", "STUDENT");
    var a =
        applications.findAll().stream()
            .filter(x -> x.getStudent().getUser().getEmail().equals("student@campusconnect.test"))
            .findFirst()
            .orElseThrow();
    render("/applications/" + a.getId(), "student@campusconnect.test", "STUDENT");
  }

  @Test
  void adminPagesAndFormsRenderOutsideTransaction() throws Exception {
    for (String url :
        List.of(
            "/dashboard",
            "/admin/students",
            "/admin/companies",
            "/admin/companies?approval=PENDING",
            "/jobs",
            "/manage/jobs/new",
            "/applications",
            "/interviews",
            "/offers",
            "/staff/drives",
            "/staff/drives/new",
            "/staff/announcements",
            "/staff/announcements/new",
            "/staff/reports",
            "/admin/audit")) render(url, "admin@campusconnect.test", "ADMIN");
    render(
        "/admin/students/" + students.findAll().getFirst().getId(),
        "admin@campusconnect.test",
        "ADMIN");
    render(
        "/admin/students/" + students.findAll().getFirst().getId() + "/edit",
        "admin@campusconnect.test",
        "ADMIN");
    render(
        "/admin/companies/" + companies.findAll().getFirst().getId(),
        "admin@campusconnect.test",
        "ADMIN");
    var a = applications.findAll().getFirst();
    render("/applications/" + a.getId() + "/interview", "admin@campusconnect.test", "ADMIN");
    render("/applications/" + a.getId() + "/offer", "admin@campusconnect.test", "ADMIN");
    render(
        "/manage/jobs/" + jobs.findAll().getFirst().getId() + "/edit",
        "admin@campusconnect.test",
        "ADMIN");
  }

  @Test
  void companyAndCoordinatorPagesRender() throws Exception {
    for (String url :
        List.of(
            "/dashboard",
            "/company/profile",
            "/jobs",
            "/applications",
            "/offers",
            "/interviews",
            "/manage/jobs/new")) render(url, "recruiter@campusconnect.test", "COMPANY");
    for (String url :
        List.of("/dashboard", "/staff/drives", "/staff/reports", "/staff/announcements"))
      render(url, "coordinator@campusconnect.test", "COORDINATOR");
  }

  @Test
  void loginAndRolesAndCsrfAreEnforced() throws Exception {
    mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "student@campusconnect.test")
                .param("password", "CampusDemo!2026"))
        .andExpect(redirectedUrl("/dashboard"));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "student@campusconnect.test")
                .param("password", "bad"))
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(get("/admin/students").with(user("student@campusconnect.test").roles("STUDENT")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/admin/companies")
                .with(user("coordinator@campusconnect.test").roles("COORDINATOR")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/student/jobs/1/apply").with(user("student@campusconnect.test").roles("STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  @Transactional
  void registrationHashesAndRejectsDuplicateEmailAndNumber() {
    var f = new RegistrationForm();
    f.setFullName("New Student");
    f.setEmail("NEW@example.test");
    f.setPassword("SecureTest!2026");
    f.setIdentifier("REG-NEW");
    var u = accounts.register(f, Role.STUDENT);
    assertEquals("new@example.test", u.getEmail());
    assertNotEquals(f.getPassword(), u.getPassword());
    assertTrue(encoder.matches(f.getPassword(), u.getPassword()));
    assertThrows(BusinessException.class, () -> accounts.register(f, Role.STUDENT));
    f.setEmail("other@example.test");
    assertThrows(BusinessException.class, () -> accounts.register(f, Role.STUDENT));
    assertThrows(BusinessException.class, () -> accounts.register(f, Role.ADMIN));
  }

  @Test
  void invalidRegistrationReturnsFieldErrors() throws Exception {
    mvc.perform(
            post("/register/student")
                .with(csrf())
                .param("fullName", "")
                .param("email", "invalid")
                .param("password", "short")
                .param("identifier", ""))
        .andExpect(status().isOk())
        .andExpect(
            model().attributeHasFieldErrors("form", "fullName", "email", "password", "identifier"));
  }

  @Test
  @Transactional
  void fullWorkflowPersistsHistoryAndPreventsDuplicatesAndForeignAccess() {
    var job = jobs.findAll().getFirst();
    as("student@campusconnect.test", Role.STUDENT);
    var a = workflow.apply(job.getId());
    assertThrows(BusinessException.class, () -> workflow.apply(job.getId()));
    as("recruiter2@campusconnect.test", Role.COMPANY);
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> workflow.transition(a.getId(), ApplicationStatus.UNDER_REVIEW, ""));
    as(job.getCompany().getUser().getEmail(), Role.COMPANY);
    assertThrows(
        BusinessException.class,
        () -> workflow.transition(a.getId(), ApplicationStatus.SELECTED, ""));
    workflow.transition(a.getId(), ApplicationStatus.UNDER_REVIEW, "Reviewing");
    workflow.transition(a.getId(), ApplicationStatus.SHORTLISTED, "Strong Java skills");
    var interview = new InterviewForm();
    interview.setScheduledAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(2));
    interview.setRoundNumber(1);
    interview.setType("Technical");
    interview.setLocation("Room 1");
    recruitment.interview(a.getId(), interview);
    workflow.transition(a.getId(), ApplicationStatus.SELECTED, "Passed interview");
    var f = new OfferForm();
    f.setSalary(new BigDecimal("900000"));
    f.setAcceptanceDeadline(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(5));
    recruitment.offer(a.getId(), f);
    var o =
        offers.findAll().stream()
            .filter(x -> x.getApplication().getId().equals(a.getId()))
            .findFirst()
            .orElseThrow();
    as("student2@campusconnect.test", Role.STUDENT);
    assertThrows(
        org.springframework.security.access.AccessDeniedException.class,
        () -> recruitment.respond(o.getId(), true));
    as("student@campusconnect.test", Role.STUDENT);
    recruitment.respond(o.getId(), true);
    assertEquals(OfferStatus.ACCEPTED, o.getStatus());
    assertNotNull(o.getRespondedAt());
    assertEquals(ApplicationStatus.OFFER_ACCEPTED, a.getStatus());
    assertEquals(7, histories.findByApplicationIdOrderByCreatedAtAsc(a.getId()).size());
    assertThrows(BusinessException.class, () -> recruitment.respond(o.getId(), false));
  }

  @Test
  @Transactional
  void declineAndExpiredOffers() {
    var o =
        offers.findAll().stream()
            .filter(x -> x.getStatus() == OfferStatus.PENDING)
            .findFirst()
            .orElseThrow();
    as(o.getApplication().getStudent().getUser().getEmail(), Role.STUDENT);
    o.setAcceptanceDeadline(Instant.now().minusSeconds(1));
    assertThrows(BusinessException.class, () -> recruitment.respond(o.getId(), true));
    o.setAcceptanceDeadline(Instant.now().plusSeconds(300));
    recruitment.respond(o.getId(), false);
    assertEquals(OfferStatus.DECLINED, o.getStatus());
    assertEquals(ApplicationStatus.OFFER_DECLINED, o.getApplication().getStatus());
  }

  @Test
  @Transactional
  void dashboardCountsDistinctPlacedStudents() {
    as("admin@campusconnect.test", Role.ADMIN);
    var accepted =
        offers.findAll().stream()
            .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
            .findFirst()
            .orElseThrow();
    var a = new Application();
    a.setStudent(accepted.getApplication().getStudent());
    a.setJob(jobs.findAll().getFirst());
    a.setStatus(ApplicationStatus.OFFER_ACCEPTED);
    applications.save(a);
    var duplicateStudentOffer = new PlacementOffer();
    duplicateStudentOffer.setApplication(a);
    duplicateStudentOffer.setSalary(new BigDecimal("1000000"));
    duplicateStudentOffer.setStatus(OfferStatus.ACCEPTED);
    offers.save(duplicateStudentOffer);
    var stats = reports.dashboard();
    assertEquals(1L, stats.get("Placed students"));
    assertEquals("12.50%", stats.get("Placement percentage"));
  }

  @Test
  void foreignCompanyRecordsAndResumeCannotBeRead() throws Exception {
    var j =
        jobs.findAll().stream()
            .filter(
                x -> x.getCompany().getUser().getEmail().equals("recruiter2@campusconnect.test"))
            .findFirst()
            .orElseThrow();
    mvc.perform(
            get("/jobs/" + j.getId()).with(user("recruiter@campusconnect.test").roles("COMPANY")))
        .andExpect(status().isForbidden());
    var a = applications.findByJobId(j.getId()).getFirst();
    mvc.perform(
            get("/applications/" + a.getId())
                .with(user("recruiter@campusconnect.test").roles("COMPANY")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/resumes/" + a.getStudent().getId())
                .with(user("student2@campusconnect.test").roles("STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void csvExportsWork() throws Exception {
    mvc.perform(get("/admin/students/export").with(user("admin@campusconnect.test").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=students.csv"));
    mvc.perform(
            get("/staff/reports/export?batch=2027")
                .with(user("coordinator@campusconnect.test").roles("COORDINATOR")))
        .andExpect(status().isOk());
    mvc.perform(
            get("/applications/export").with(user("recruiter@campusconnect.test").roles("COMPANY")))
        .andExpect(status().isOk());
  }

  @Test
  @Transactional
  void resumeRejectsDisguisedFilesAndAllowsPrivateDownload() throws Exception {
    var invalid =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "virus.pdf", "application/pdf", "MZ executable".getBytes());
    mvc.perform(
            multipart("/student/resume")
                .file(invalid)
                .with(user("student@campusconnect.test").roles("STUDENT"))
                .with(csrf()))
        .andExpect(status().isBadRequest());
    var valid =
        new org.springframework.mock.web.MockMultipartFile(
            "file",
            "resume.pdf",
            "application/pdf",
            "%PDF-1.7\nDemo test document\n%%EOF".getBytes());
    mvc.perform(
            multipart("/student/resume")
                .file(valid)
                .with(user("student@campusconnect.test").roles("STUDENT"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    var id =
        students
            .findByUserId(users.findByEmail("student@campusconnect.test").orElseThrow().getId())
            .orElseThrow()
            .getId();
    mvc.perform(get("/resumes/" + id).with(user("student@campusconnect.test").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=resume.pdf"));
    mvc.perform(get("/resumes/" + id).with(user("student2@campusconnect.test").roles("STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  @Transactional
  void jobSubmissionRechecksClosedAndDeadlineStates() {
    var j = jobs.findAll().getFirst();
    as("student@campusconnect.test", Role.STUDENT);
    j.setStatus(JobStatus.CLOSED);
    assertThrows(BusinessException.class, () -> workflow.apply(j.getId()));
    j.setStatus(JobStatus.PUBLISHED);
    j.setDeadline(Instant.now().minusSeconds(1));
    assertThrows(BusinessException.class, () -> workflow.apply(j.getId()));
  }

  @Test
  @Transactional
  void databaseUniquePairPreventsBypassingApplicationService() {
    var original = applications.findAll().getFirst();
    var duplicate = new Application();
    duplicate.setStudent(original.getStudent());
    duplicate.setJob(original.getJob());
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> applications.saveAndFlush(duplicate));
  }

  @Test
  void invalidJobDatesReturnUsableFormInsteadOfTransactionError() throws Exception {
    var company =
        companies
            .findByUserId(users.findByEmail("recruiter@campusconnect.test").orElseThrow().getId())
            .orElseThrow();
    mvc.perform(
            post("/manage/jobs/new")
                .with(user("recruiter@campusconnect.test").roles("COMPANY"))
                .with(csrf())
                .param("companyId", company.getId().toString())
                .param("title", "Invalid date job")
                .param("description", "Description")
                .param("requiredSkills", "Java")
                .param("category", "Software")
                .param("location", "Pune")
                .param("employmentType", "FULL_TIME")
                .param("salaryMin", "100")
                .param("salaryMax", "200")
                .param("minimumCgpa", "7")
                .param("maximumBacklogs", "0")
                .param("departments", "CSE")
                .param("batches", "2027")
                .param("opensAt", "2030-06-02T10:00")
                .param("deadline", "2030-06-01T10:00")
                .param("vacancies", "2")
                .param("status", "DRAFT"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasErrors("form"));
  }

  @Test
  @Transactional
  void administratorCanMaintainDepartmentsAndCreateCoordinators() throws Exception {
    render("/admin/departments", "admin@campusconnect.test", "ADMIN");
    render("/admin/departments/new", "admin@campusconnect.test", "ADMIN");
    render("/admin/coordinators/new", "admin@campusconnect.test", "ADMIN");
    mvc.perform(
            post("/admin/departments/new")
                .with(user("admin@campusconnect.test").roles("ADMIN"))
                .with(csrf())
                .param("code", "BIOTECH")
                .param("name", "Biotechnology"))
        .andExpect(redirectedUrl("/admin/departments"));
    assertTrue(running.getBean(DepartmentRepository.class).findByCode("BIOTECH").isPresent());
    mvc.perform(
            post("/admin/coordinators/new")
                .with(user("admin@campusconnect.test").roles("ADMIN"))
                .with(csrf())
                .param("fullName", "New Coordinator")
                .param("email", "new.coordinator@example.test")
                .param("password", "Configured!2026"))
        .andExpect(redirectedUrl("/admin/coordinators"));
    assertEquals(
        Role.COORDINATOR,
        users.findByEmail("new.coordinator@example.test").orElseThrow().getRole());
    mvc.perform(
            post("/admin/departments/new")
                .with(user("coordinator@campusconnect.test").roles("COORDINATOR"))
                .with(csrf())
                .param("code", "OTHER")
                .param("name", "Other"))
        .andExpect(status().isForbidden());
  }
}
