package edu.campusconnect.service;

import edu.campusconnect.dto.TableRow;
import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.repository.*;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {
  private final StudentProfileRepository students;
  private final CompanyRepository companies;
  private final JobRepository jobs;
  private final ApplicationRepository applications;
  private final PlacementOfferRepository offers;
  private final AccessService access;
  private final java.time.ZoneId applicationZone;
  private final edu.campusconnect.repository.DepartmentRepository departmentRepository;
  private final PlacementDriveRepository drives;

  @org.springframework.beans.factory.annotation.Value("${app.salary-buckets}")
  private int bucketCount;

  public static BigDecimal percentage(long placed, long eligible) {
    return eligible == 0
        ? BigDecimal.ZERO
        : BigDecimal.valueOf(placed)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(eligible), 2, RoundingMode.HALF_UP);
  }

  public static boolean eligible(StudentProfile s) {
    return s.getUser().isEnabled()
        && s.getDepartment() != null
        && s.getGraduationYear() != null
        && s.getCgpa() != null
        && s.getBacklogs() != null;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> dashboard() {
    var u = access.current();
    var all = applications.findAll();
    var visible = all.stream().filter(a -> access.sees(u, a)).toList();
    var os = offers.findAll().stream().filter(o -> access.sees(u, o.getApplication())).toList();
    var js =
        jobs.findAll().stream()
            .filter(j -> u.getRole() != Role.COMPANY || access.manages(u, j))
            .toList();
    var m = new LinkedHashMap<String, Object>();
    if (access.staff(u)) {
      var ss = students.findAll();
      long eligible = ss.stream().filter(ReportService::eligible).count();
      long placed =
          os.stream()
              .filter(
                  o ->
                      o.getStatus() == OfferStatus.ACCEPTED
                          && eligible(o.getApplication().getStudent()))
              .map(o -> o.getApplication().getStudent().getId())
              .distinct()
              .count();
      m.put("Registered students", ss.size());
      m.put("Eligible students", eligible);
      m.put(
          "Approved companies",
          companies.findAll().stream()
              .filter(c -> c.getApproval() == ApprovalState.APPROVED && c.getUser().isEnabled())
              .count());
      m.put("Placed students", placed);
      m.put("Placement percentage", percentage(placed, eligible) + "%");
    }
    m.put("Open jobs", js.stream().filter(Job::isOpen).count());
    if (access.staff(u)) {
      var today = java.time.LocalDate.now(applicationZone);
      m.put(
          "Active placement drives",
          drives.findAll().stream()
              .filter(d -> !today.isBefore(d.getStartDate()) && !today.isAfter(d.getEndDate()))
              .count());
    }
    m.put("Applications", visible.size());
    m.put(
        "Distinct applicants",
        visible.stream().map(a -> a.getStudent().getId()).distinct().count());
    m.put("Offers", os.size());
    m.put(
        "Accepted offers", os.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count());
    return m;
  }

  @Transactional(readOnly = true)
  public List<TableRow> departments(Integer batch, Long companyId, String academicYear) {
    access.roles(Role.ADMIN, Role.COORDINATOR);
    var ss =
        students.findAll().stream()
            .filter(ReportService::eligible)
            .filter(s -> batch == null || batch.equals(s.getGraduationYear()))
            .toList();
    var os =
        offers.findAll().stream()
            .filter(
                o ->
                    batch == null
                        || batch.equals(o.getApplication().getStudent().getGraduationYear()))
            .filter(
                o ->
                    companyId == null
                        || companyId.equals(o.getApplication().getJob().getCompany().getId()))
            .filter(
                o ->
                    academicYear == null
                        || academicYear.isBlank()
                        || (o.getApplication().getJob().getDrive() != null
                            && academicYear.equals(
                                o.getApplication().getJob().getDrive().getAcademicYear())))
            .toList();
    var rows = new ArrayList<TableRow>();
    for (var d : departmentRepository.findAll()) {
      long eligible = ss.stream().filter(s -> d.equals(s.getDepartment())).count();
      var accepted =
          os.stream()
              .filter(
                  o ->
                      o.getStatus() == OfferStatus.ACCEPTED
                          && d.equals(o.getApplication().getStudent().getDepartment())
                          && eligible(o.getApplication().getStudent()))
              .toList();
      long placed =
          accepted.stream().map(o -> o.getApplication().getStudent().getId()).distinct().count();
      var salaries =
          os.stream()
              .filter(o -> d.equals(o.getApplication().getStudent().getDepartment()))
              .map(PlacementOffer::getSalary)
              .toList();
      BigDecimal sum = salaries.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      rows.add(
          TableRow.of(
              null,
              d,
              eligible,
              placed,
              percentage(placed, eligible) + "%",
              salaries.size(),
              salaries.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
              salaries.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
              salaries.isEmpty()
                  ? BigDecimal.ZERO
                  : sum.divide(BigDecimal.valueOf(salaries.size()), 2, RoundingMode.HALF_UP)));
    }
    return rows;
  }

  @Transactional(readOnly = true)
  public List<TableRow> companyReport() {
    access.roles(Role.ADMIN, Role.COORDINATOR);
    var apps = applications.findAll();
    var os = offers.findAll();
    return companies.findAll().stream()
        .map(
            c ->
                TableRow.of(
                    null,
                    c.getName(),
                    apps.stream()
                        .filter(a -> a.getJob().getCompany().getId().equals(c.getId()))
                        .count(),
                    os.stream()
                        .filter(
                            o -> o.getApplication().getJob().getCompany().getId().equals(c.getId()))
                        .count(),
                    os.stream()
                        .filter(
                            o ->
                                o.getApplication().getJob().getCompany().getId().equals(c.getId())
                                    && o.getStatus() == OfferStatus.ACCEPTED)
                        .map(o -> o.getApplication().getStudent().getId())
                        .distinct()
                        .count()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TableRow> salaryDistribution(Integer batch, Long companyId, String academicYear) {
    access.roles(Role.ADMIN, Role.COORDINATOR);
    var amounts =
        offers.findAll().stream()
            .filter(
                o ->
                    batch == null
                        || batch.equals(o.getApplication().getStudent().getGraduationYear()))
            .filter(
                o ->
                    companyId == null
                        || companyId.equals(o.getApplication().getJob().getCompany().getId()))
            .filter(
                o ->
                    academicYear == null
                        || academicYear.isBlank()
                        || (o.getApplication().getJob().getDrive() != null
                            && academicYear.equals(
                                o.getApplication().getJob().getDrive().getAcademicYear())))
            .map(PlacementOffer::getSalary)
            .toList();
    if (amounts.isEmpty()) return List.of();
    var minimum = amounts.stream().min(BigDecimal::compareTo).orElseThrow();
    var maximum = amounts.stream().max(BigDecimal::compareTo).orElseThrow();
    if (minimum.compareTo(maximum) == 0)
      return List.of(TableRow.of(null, minimum, amounts.size(), 100));
    int count = Math.max(1, Math.min(bucketCount, 20));
    var width =
        maximum.subtract(minimum).divide(BigDecimal.valueOf(count), 2, RoundingMode.CEILING);
    long[] counts = new long[count];
    for (var amount : amounts)
      counts[
          Math.min(count - 1, amount.subtract(minimum).divideToIntegralValue(width).intValue())]++;
    var rows = new ArrayList<TableRow>();
    for (int n = 0; n < count; n++) {
      var lower = minimum.add(width.multiply(BigDecimal.valueOf(n)));
      var upper = n == count - 1 ? maximum : lower.add(width);
      rows.add(
          TableRow.of(
              null,
              "[" + lower + ", " + upper + (n == count - 1 ? "]" : ")"),
              counts[n],
              percentage(counts[n], amounts.size())));
    }
    return rows;
  }
}
