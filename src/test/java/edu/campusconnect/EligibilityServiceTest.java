package edu.campusconnect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.repository.ApplicationRepository;
import edu.campusconnect.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class EligibilityServiceTest {
  static Department department(String code) {
    var d = new Department();
    d.setId((long) code.hashCode());
    d.setCode(code);
    d.setName(code);
    return d;
  }

  ApplicationRepository repository;
  EligibilityService service;
  StudentProfile student;
  Job job;
  Instant now = Instant.parse("2026-10-29T10:00:00Z");

  @BeforeEach
  void setup() {
    repository = mock(ApplicationRepository.class);
    service = new EligibilityService(repository, Clock.fixed(now, ZoneOffset.UTC));
    var u = new User();
    u.setId(1L);
    student = new StudentProfile();
    student.setId(2L);
    student.setUser(u);
    student.setCgpa(new BigDecimal("8"));
    student.setDepartment(department("CSE"));
    student.setGraduationYear(2027);
    student.setBacklogs(0);
    var company = new Company();
    company.setUser(u);
    company.setApproval(ApprovalState.APPROVED);
    job = new Job();
    job.setId(3L);
    job.setCompany(company);
    job.setMinimumCgpa(new BigDecimal("7"));
    job.setDepartments(Set.of(department("CSE")));
    job.setBatches(Set.of(2027));
    job.setMaximumBacklogs(0);
    job.setOpensAt(now.minusSeconds(60));
    job.setDeadline(now.plusSeconds(60));
    job.setStatus(JobStatus.PUBLISHED);
  }

  @Test
  void eligibleAtOpeningBoundary() {
    job.setOpensAt(now);
    assertTrue(service.reasons(student, job).isEmpty());
  }

  @Test
  void deadlineIsExclusive() {
    job.setDeadline(now);
    assertTrue(service.reasons(student, job).contains("The application deadline has passed"));
  }

  @Test
  void futureOpeningIsRejected() {
    job.setOpensAt(now.plusSeconds(1));
    assertFalse(service.reasons(student, job).isEmpty());
  }

  @Test
  void unpublishedAndUnapprovedRejected() {
    job.setStatus(JobStatus.DRAFT);
    assertFalse(service.reasons(student, job).isEmpty());
    job.setStatus(JobStatus.PUBLISHED);
    job.getCompany().setApproval(ApprovalState.PENDING);
    assertFalse(service.reasons(student, job).isEmpty());
  }

  @Test
  void reportsAllAcademicFailures() {
    student.setCgpa(new BigDecimal("6"));
    student.setDepartment(department("CIVIL"));
    student.setGraduationYear(2030);
    student.setBacklogs(2);
    assertEquals(4, service.reasons(student, job).size());
  }

  @Test
  void missingProfileDoesNotPass() {
    student.setCgpa(null);
    student.setDepartment(null);
    student.setGraduationYear(null);
    student.setBacklogs(null);
    assertEquals(4, service.reasons(student, job).size());
  }

  @Test
  void duplicateApplicationRejected() {
    when(repository.existsByStudentIdAndJobId(2L, 3L)).thenReturn(true);
    assertTrue(service.reasons(student, job).contains("You have already applied"));
  }

  @Test
  void workflowDoesNotSkipStagesOrReviveFinalState() {
    assertTrue(
        ApplicationService.next(ApplicationStatus.APPLIED)
            .contains(ApplicationStatus.UNDER_REVIEW));
    assertFalse(
        ApplicationService.next(ApplicationStatus.APPLIED).contains(ApplicationStatus.OFFERED));
    assertTrue(ApplicationService.next(ApplicationStatus.OFFER_ACCEPTED).isEmpty());
    assertFalse(ApplicationService.withdrawable(ApplicationStatus.OFFERED));
  }

  @Test
  void percentageHandlesEmptyAndRounding() {
    assertEquals(BigDecimal.ZERO, ReportService.percentage(0, 0));
    assertEquals(new BigDecimal("33.33"), ReportService.percentage(1, 3));
  }

  @Test
  void csvNeutralizesSpreadsheetFormulasAndEscapesQuotes() {
    assertEquals("\"'=SUM(1,2)\"", edu.campusconnect.util.Csv.cell("=SUM(1,2)"));
    assertEquals("\"a\"\"b\"", edu.campusconnect.util.Csv.cell("a\"b"));
  }
}
