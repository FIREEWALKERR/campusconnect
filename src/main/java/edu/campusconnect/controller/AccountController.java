package edu.campusconnect.controller;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountController {
  private final AccessService access;
  private final AccountService accounts;
  private final StudentProfileRepository students;
  private final CompanyRepository companies;
  private final ResumeService resumes;
  private final ReportService reports;
  private final NotificationRepository notifications;
  private final AnnouncementRepository announcements;

  @GetMapping("/dashboard")
  public String dashboard(Model m) {
    m.addAttribute("stats", reports.dashboard());
    m.addAttribute(
        "announcements",
        announcements
            .findByPublishedAtLessThanEqualOrderByPublishedAtDesc(java.time.Instant.now())
            .stream()
            .limit(3)
            .toList());
    if (access.current().getRole() == Role.STUDENT)
      m.addAttribute("profile", students.findByUserId(access.current().getId()).orElseThrow());
    return "dashboard";
  }

  @GetMapping("/student/profile")
  public String profile(Model m) {
    m.addAttribute("profile", students.findByUserId(access.current().getId()).orElseThrow());
    return "profile";
  }

  @GetMapping({"/student/profile/edit", "/admin/students/{id}/edit"})
  public String edit(@PathVariable(required = false) Long id, Model m) {
    var s =
        id == null
            ? students.findByUserId(access.current().getId()).orElseThrow()
            : students.findById(id).orElseThrow();
    if (id != null) access.roles(Role.ADMIN);
    var f = new ProfileForm();
    BeanUtils.copyProperties(s, f, "skills");
    f.setFullName(s.getUser().getFullName());
    f.setSkills(
        s.getSkills().stream()
            .map(Skill::getName)
            .collect(java.util.stream.Collectors.joining(", ")));
    m.addAttribute("form", f);
    m.addAttribute("title", "Edit student profile");
    return "forms/profile";
  }

  @PostMapping({"/student/profile/edit", "/admin/students/{id}/edit"})
  @Transactional
  public String edit(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") ProfileForm f,
      BindingResult errors,
      Model m,
      RedirectAttributes ra) {
    m.addAttribute("title", "Edit student profile");
    if (errors.hasErrors()) return "forms/profile";
    Long target =
        id == null ? students.findByUserId(access.current().getId()).orElseThrow().getId() : id;
    accounts.profile(target, f);
    ra.addFlashAttribute("success", "Profile saved successfully");
    return id == null ? "redirect:/student/profile" : "redirect:/admin/students/" + id;
  }

  @PostMapping("/student/resume")
  @Transactional
  public String upload(@RequestParam MultipartFile file, RedirectAttributes ra)
      throws java.io.IOException {
    resumes.upload(file);
    ra.addFlashAttribute("success", "Resume uploaded securely");
    return "redirect:/student/profile";
  }

  @GetMapping("/resumes/{id}")
  public ResponseEntity<Resource> download(@PathVariable Long id) {
    var path = resumes.download(id);
    boolean pdf = path.toString().endsWith(".pdf");
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=resume." + (pdf ? "pdf" : "docx"))
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.noStore())
        .contentType(
            MediaType.parseMediaType(
                pdf
                    ? "application/pdf"
                    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .body(new FileSystemResource(path));
  }

  @GetMapping("/company/profile")
  public String company(Model m) {
    var c = companies.findByUserId(access.current().getId()).orElseThrow();
    var f = new CompanyForm();
    BeanUtils.copyProperties(c, f);
    m.addAttribute("form", f);
    m.addAttribute("title", "Company profile · " + c.getApproval());
    return "forms/company";
  }

  @PostMapping("/company/profile")
  @Transactional
  public String company(
      @Valid @ModelAttribute("form") CompanyForm f,
      BindingResult errors,
      Model m,
      RedirectAttributes ra) {
    m.addAttribute("title", "Company profile");
    if (errors.hasErrors()) return "forms/company";
    accounts.company(f);
    ra.addFlashAttribute("success", "Company profile saved");
    return "redirect:/company/profile";
  }

  @GetMapping("/account")
  public String account(Model m) {
    m.addAttribute("form", new PasswordForm());
    m.addAttribute("title", "Account settings");
    return "forms/password";
  }

  @PostMapping("/account")
  @Transactional
  public String password(
      @Valid @ModelAttribute("form") PasswordForm f,
      BindingResult errors,
      Model m,
      RedirectAttributes ra) {
    m.addAttribute("title", "Account settings");
    if (errors.hasErrors()) return "forms/password";
    accounts.password(f);
    ra.addFlashAttribute("success", "Password changed");
    return "redirect:/account";
  }

  @GetMapping("/notifications")
  public String notifications(Model m) {
    m.addAttribute(
        "notifications", notifications.findByUserIdOrderByCreatedAtDesc(access.current().getId()));
    return "notifications";
  }

  @PostMapping("/notifications/{id}/read")
  @Transactional
  public String read(@PathVariable Long id) {
    var n = notifications.findById(id).orElseThrow();
    if (!n.getUser().getId().equals(access.current().getId()))
      throw new org.springframework.security.access.AccessDeniedException("Private notification");
    n.setReadFlag(true);
    return "redirect:/notifications";
  }

  @GetMapping("/announcements")
  public String announcements(Model m) {
    m.addAttribute(
        "announcements",
        announcements.findByPublishedAtLessThanEqualOrderByPublishedAtDesc(
            java.time.Instant.now()));
    return "announcements";
  }
}
