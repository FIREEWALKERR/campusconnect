package edu.campusconnect.controller;

import edu.campusconnect.dto.TableRow;
import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffController {
  private final StudentProfileRepository students;
  private final CompanyRepository companies;
  private final PlacementDriveRepository drives;
  private final AnnouncementRepository announcements;
  private final AuditLogRepository audits;
  private final AccountService accounts;
  private final AccessService access;

  @org.springframework.beans.factory.annotation.Value("${app.currency}")
  private String currency;

  private final java.time.ZoneId applicationZone;
  private final StaffService staff;
  private final ReportService reports;

  private List<TableRow> studentRows(String q, Department department, Integer batch, String sort) {
    var stream =
        students.findAll().stream()
            .filter(
                s ->
                    (s.getUser().getFullName()
                            + " "
                            + s.getUser().getEmail()
                            + " "
                            + s.getRegistrationNumber())
                        .toLowerCase(Locale.ROOT)
                        .contains(q.toLowerCase(Locale.ROOT)))
            .filter(s -> department == null || department.equals(s.getDepartment()))
            .filter(s -> batch == null || batch.equals(s.getGraduationYear()));
    Comparator<StudentProfile> comparator =
        sort.equals("registration")
            ? Comparator.comparing(StudentProfile::getRegistrationNumber)
            : Comparator.comparing(s -> s.getUser().getFullName());
    return stream
        .sorted(comparator)
        .map(
            s ->
                TableRow.of(
                    "/admin/students/" + s.getId(),
                    s.getUser().getFullName(),
                    s.getRegistrationNumber(),
                    s.getDepartment(),
                    s.getGraduationYear(),
                    s.getCgpa(),
                    s.getUser().isEnabled() ? "Active" : "Disabled"))
        .toList();
  }

  @GetMapping("/admin/students")
  public String students(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Department department,
      @RequestParam(required = false) Integer batch,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    m.addAttribute("studentFilters", true);
    m.addAttribute("department", department);
    m.addAttribute("batch", batch);
    m.addAttribute("sort", sort);
    return TableSupport.show(
        m,
        "Students",
        List.of("Student", "Registration", "Department", "Batch", "CGPA", "Account"),
        studentRows(q, department, batch, sort),
        page,
        q,
        null,
        "/admin/students/export");
  }

  @GetMapping("/admin/students/export")
  public ResponseEntity<String> exportStudents(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Department department,
      @RequestParam(required = false) Integer batch,
      @RequestParam(defaultValue = "name") String sort) {
    return TableSupport.csv(
        "students",
        List.of("Student", "Registration", "Department", "Batch", "CGPA", "Account"),
        studentRows(q, department, batch, sort));
  }

  @GetMapping("/admin/students/{id}")
  public String student(@PathVariable Long id, Model m) {
    m.addAttribute("profile", students.findById(id).orElseThrow());
    return "profile";
  }

  @PostMapping("/admin/users/{id}/enabled")
  @Transactional
  public String enabled(@PathVariable Long id, @RequestParam boolean enabled) {
    accounts.enabled(id, enabled);
    return "redirect:/admin/students";
  }

  @GetMapping("/admin/companies")
  public String companies(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) ApprovalState approval,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    m.addAttribute("approvalStates", ApprovalState.values());
    m.addAttribute("selectedApproval", approval);
    var rows =
        companies.findAll().stream()
            .filter(c -> c.getName().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
            .filter(c -> approval == null || c.getApproval() == approval)
            .map(
                c ->
                    TableRow.of(
                        "/admin/companies/" + c.getId(),
                        c.getName(),
                        c.getUser().getEmail(),
                        c.getLocation(),
                        c.getApproval()))
            .toList();
    return TableSupport.show(
        m,
        "Companies & approvals",
        List.of("Company", "Contact email", "Location", "Approval"),
        rows,
        page,
        q,
        null,
        null);
  }

  @GetMapping("/admin/companies/{id}")
  public String company(@PathVariable Long id, Model m) {
    m.addAttribute("company", companies.findById(id).orElseThrow());
    return "company";
  }

  @PostMapping("/admin/companies/{id}/approval")
  @Transactional
  public String approval(@PathVariable Long id, @RequestParam ApprovalState approval) {
    accounts.approve(id, approval);
    return "redirect:/admin/companies/" + id;
  }

  @GetMapping("/staff/drives")
  public String drives(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    return TableSupport.show(
        m,
        "Placement drives",
        List.of("Drive", "Academic year", "Batch", "Starts", "Ends", "Venue"),
        drives.findAll().stream()
            .filter(d -> d.getName().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
            .map(
                d ->
                    TableRow.of(
                        "/staff/drives/" + d.getId() + "/edit",
                        d.getName(),
                        d.getAcademicYear(),
                        d.getBatch(),
                        d.getStartDate(),
                        d.getEndDate(),
                        d.getVenue()))
            .toList(),
        page,
        q,
        "/staff/drives/new",
        null);
  }

  @GetMapping({"/staff/drives/new", "/staff/drives/{id}/edit"})
  public String drive(@PathVariable(required = false) Long id, Model m) {
    var f = new DriveForm();
    if (id != null) BeanUtils.copyProperties(drives.findById(id).orElseThrow(), f);
    m.addAttribute("form", f);
    m.addAttribute("title", "Placement drive");
    return "forms/drive";
  }

  @PostMapping({"/staff/drives/new", "/staff/drives/{id}/edit"})
  @Transactional
  public String drive(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") DriveForm f,
      BindingResult errors,
      Model m) {
    m.addAttribute("title", "Placement drive");
    if (errors.hasErrors()) return "forms/drive";
    staff.drive(id, f);
    return "redirect:/staff/drives";
  }

  @GetMapping("/staff/announcements")
  public String announcementList(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    return TableSupport.show(
        m,
        "Manage announcements",
        List.of("Title", "Publication (UTC)", "Author"),
        announcements.findAll().stream()
            .filter(a -> a.getTitle().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(Announcement::getPublishedAt).reversed())
            .map(
                a ->
                    TableRow.of(
                        "/staff/announcements/" + a.getId() + "/edit",
                        a.getTitle(),
                        a.getPublishedAt(),
                        a.getAuthor().getFullName()))
            .toList(),
        page,
        q,
        "/staff/announcements/new",
        null);
  }

  @GetMapping({"/staff/announcements/new", "/staff/announcements/{id}/edit"})
  public String announcement(@PathVariable(required = false) Long id, Model m) {
    var f = new AnnouncementForm();
    f.setPublishedAt(LocalDateTime.now(applicationZone));
    if (id != null) {
      var a = announcements.findById(id).orElseThrow();
      BeanUtils.copyProperties(a, f, "publishedAt");
      f.setPublishedAt(LocalDateTime.ofInstant(a.getPublishedAt(), applicationZone));
    }
    m.addAttribute("form", f);
    m.addAttribute("title", "Publish announcement");
    m.addAttribute("recordId", id);
    return "forms/announcement";
  }

  @PostMapping({"/staff/announcements/new", "/staff/announcements/{id}/edit"})
  @Transactional
  public String announcement(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") AnnouncementForm f,
      BindingResult errors,
      Model m) {
    m.addAttribute("title", "Publish announcement");
    if (errors.hasErrors()) return "forms/announcement";
    staff.announcement(id, f);
    return "redirect:/staff/announcements";
  }

  @PostMapping("/staff/announcements/{id}/delete")
  @Transactional
  public String deleteAnnouncement(@PathVariable Long id) {
    staff.deleteAnnouncement(id);
    return "redirect:/staff/announcements";
  }

  @GetMapping("/admin/audit")
  public String audit(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    return TableSupport.show(
        m,
        "Audit history",
        List.of("Action", "Target ID", "Actor", "Timestamp (UTC)"),
        audits.findAll().stream()
            .filter(
                a -> a.getAction().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
            .map(
                a ->
                    TableRow.of(
                        null,
                        a.getAction(),
                        a.getTarget(),
                        a.getActor().getEmail(),
                        a.getCreatedAt()))
            .toList(),
        page,
        q,
        null,
        null);
  }

  private List<String> reportHeaders() {
    return REPORT_HEADERS.stream().map(h -> h.replace("{currency}", currency)).toList();
  }

  private static final List<String> REPORT_HEADERS =
      List.of(
          "Department",
          "Eligible students",
          "Placed students",
          "Placement %",
          "Offers",
          "Lowest CTC {currency}",
          "Highest CTC {currency}",
          "Average CTC {currency}");

  @GetMapping("/staff/reports")
  public String reports(
      @RequestParam(required = false) Integer batch,
      @RequestParam(required = false) Long companyId,
      @RequestParam(defaultValue = "") String academicYear,
      Model m) {
    m.addAttribute("headers", reportHeaders());
    m.addAttribute("rows", reports.departments(batch, companyId, academicYear));
    m.addAttribute("companyRows", reports.companyReport());
    m.addAttribute("distribution", reports.salaryDistribution(batch, companyId, academicYear));
    m.addAttribute("companies", companies.findAll());
    m.addAttribute("batch", batch);
    m.addAttribute("companyId", companyId);
    m.addAttribute("academicYear", academicYear);
    return "reports";
  }

  @GetMapping("/staff/reports/export")
  public ResponseEntity<String> export(
      @RequestParam(required = false) Integer batch,
      @RequestParam(required = false) Long companyId,
      @RequestParam(defaultValue = "") String academicYear) {
    return TableSupport.csv(
        "placement-report", reportHeaders(), reports.departments(batch, companyId, academicYear));
  }
}
