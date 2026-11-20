package edu.campusconnect.controller;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobController {
  private final JobRepository jobs;
  private final CompanyRepository companies;
  private final PlacementDriveRepository drives;
  private final StudentProfileRepository students;
  private final AccessService access;
  private final java.time.ZoneId applicationZone;
  private final JobService service;
  private final EligibilityService eligibility;
  private final ApplicationService applications;

  @GetMapping("/jobs")
  public String list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String location,
      @RequestParam(required = false) BigDecimal salary,
      @RequestParam(required = false) Department department,
      @RequestParam(defaultValue = "false") boolean eligibleOnly,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "deadline") String sort,
      Model m) {
    var u = access.current();
    var now = Instant.now();
    Specification<Job> spec =
        (r, c, b) -> {
          var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
          if (u.getRole() == Role.STUDENT) {
            p.add(b.equal(r.get("status"), JobStatus.PUBLISHED));
            p.add(b.lessThanOrEqualTo(r.get("opensAt"), now));
            p.add(b.greaterThan(r.get("deadline"), now));
            p.add(b.equal(r.get("company").get("approval"), ApprovalState.APPROVED));
            p.add(b.isTrue(r.get("company").get("user").get("enabled")));
          }
          if (u.getRole() == Role.COMPANY)
            p.add(b.equal(r.get("company").get("user").get("id"), u.getId()));
          if (!q.isBlank())
            p.add(
                b.or(
                    b.like(b.lower(r.get("title")), "%" + q.toLowerCase(Locale.ROOT) + "%"),
                    b.like(
                        b.lower(r.get("company").get("name")),
                        "%" + q.toLowerCase(Locale.ROOT) + "%")));
          if (!location.isBlank())
            p.add(
                b.like(b.lower(r.get("location")), "%" + location.toLowerCase(Locale.ROOT) + "%"));
          if (salary != null) p.add(b.greaterThanOrEqualTo(r.get("salaryMax"), salary));
          if (department != null) p.add(b.isMember(department, r.get("departments")));
          if (eligibleOnly && u.getRole() == Role.STUDENT) {
            var s = students.findByUserId(u.getId()).orElseThrow();
            if (s.getCgpa() == null
                || s.getDepartment() == null
                || s.getGraduationYear() == null
                || s.getBacklogs() == null) p.add(b.disjunction());
            else {
              p.add(b.lessThanOrEqualTo(r.get("minimumCgpa"), s.getCgpa()));
              p.add(b.greaterThanOrEqualTo(r.get("maximumBacklogs"), s.getBacklogs()));
              p.add(b.isMember(s.getDepartment(), r.get("departments")));
              p.add(b.isMember(s.getGraduationYear(), r.get("batches")));
              var sub = c.subquery(Long.class);
              var a = sub.from(Application.class);
              sub.select(a.get("id"))
                  .where(
                      b.equal(a.get("student").get("id"), s.getId()),
                      b.equal(a.get("job").get("id"), r.get("id")));
              p.add(b.not(b.exists(sub)));
            }
          }
          return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    String field = Set.of("deadline", "title", "salaryMax").contains(sort) ? sort : "deadline";
    var result =
        jobs.findAll(spec, PageRequest.of(Math.max(0, page), 9, Sort.by(field).ascending()));
    m.addAttribute("jobs", result.getContent());
    m.addAttribute("page", result);
    m.addAttribute("q", q);
    m.addAttribute("location", location);
    m.addAttribute("salary", salary);
    m.addAttribute("department", department);
    m.addAttribute("eligibleOnly", eligibleOnly);
    m.addAttribute("sort", field);
    return "jobs";
  }

  @GetMapping("/jobs/{id}")
  public String detail(@PathVariable Long id, Model m) {
    var j = jobs.findById(id).orElseThrow();
    if (access.current().getRole() == Role.STUDENT) {
      if (!j.isOpen())
        throw new edu.campusconnect.exception.BusinessException(
            "This job is not currently open. Your existing applications remain available under"
                + " Applications.");
      m.addAttribute(
          "reasons",
          eligibility.reasons(students.findByUserId(access.current().getId()).orElseThrow(), j));
    } else access.manage(j);
    m.addAttribute("job", j);
    return "job";
  }

  private void options(Model m) {
    var u = access.current();
    m.addAttribute(
        "companies",
        companies.findAll().stream()
            .filter(c -> access.staff(u) || c.getUser().getId().equals(u.getId()))
            .toList());
    m.addAttribute("drives", drives.findAll());
    m.addAttribute("title", "Job opening");
  }

  @GetMapping({"/manage/jobs/new", "/manage/jobs/{id}/edit"})
  public String edit(@PathVariable(required = false) Long id, Model m) {
    access.roles(Role.ADMIN, Role.COORDINATOR, Role.COMPANY);
    var f = new JobForm();
    if (id != null) {
      var j = jobs.findById(id).orElseThrow();
      access.manage(j);
      BeanUtils.copyProperties(j, f, "opensAt", "deadline");
      f.setCompanyId(j.getCompany().getId());
      f.setDriveId(j.getDrive() == null ? null : j.getDrive().getId());
      f.setOpensAt(LocalDateTime.ofInstant(j.getOpensAt(), applicationZone));
      f.setDeadline(LocalDateTime.ofInstant(j.getDeadline(), applicationZone));
    } else {
      f.setOpensAt(LocalDateTime.now(applicationZone));
      f.setDeadline(f.getOpensAt().plusDays(14));
    }
    m.addAttribute("form", f);
    options(m);
    return "forms/job";
  }

  @PostMapping({"/manage/jobs/new", "/manage/jobs/{id}/edit"})
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public String edit(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") JobForm f,
      BindingResult errors,
      Model m,
      RedirectAttributes ra) {
    access.roles(Role.ADMIN, Role.COORDINATOR, Role.COMPANY);
    options(m);
    if (errors.hasErrors()) return "forms/job";
    try {
      var j = service.save(id, f);
      ra.addFlashAttribute("success", "Job saved");
      return "redirect:/jobs/" + j.getId();
    } catch (edu.campusconnect.exception.BusinessException e) {
      errors.reject("job", e.getMessage());
      return "forms/job";
    }
  }

  @PostMapping("/manage/jobs/{id}/delete")
  @Transactional
  public String delete(@PathVariable Long id, RedirectAttributes ra) {
    service.delete(id);
    ra.addFlashAttribute("success", "Draft deleted");
    return "redirect:/jobs";
  }

  @PostMapping("/student/jobs/{id}/apply")
  @Transactional
  public String apply(@PathVariable Long id, RedirectAttributes ra) {
    var a = applications.apply(id);
    ra.addFlashAttribute("success", "Application submitted");
    return "redirect:/applications/" + a.getId();
  }
}
