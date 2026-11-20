package edu.campusconnect.controller;

import edu.campusconnect.dto.TableRow;
import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.*;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentController {
  private final ApplicationRepository applications;
  private final ApplicationStatusHistoryRepository history;
  private final InterviewRepository interviews;
  private final PlacementOfferRepository offers;
  private final AccessService access;
  private final ApplicationService workflow;
  private final RecruitmentService recruitment;

  private List<Application> visible(String q, ApplicationStatus status, Long jobId) {
    var u = access.current();
    return applications.findAll().stream()
        .filter(a -> access.sees(u, a))
        .filter(a -> status == null || a.getStatus() == status)
        .filter(a -> jobId == null || jobId.equals(a.getJob().getId()))
        .filter(
            a ->
                (a.getStudent().getUser().getFullName()
                        + " "
                        + a.getJob().getTitle()
                        + " "
                        + a.getJob().getCompany().getName())
                    .toLowerCase(Locale.ROOT)
                    .contains(q.toLowerCase(Locale.ROOT)))
        .sorted(Comparator.comparing(Application::getCreatedAt).reversed())
        .toList();
  }

  private List<TableRow> rows(List<Application> apps) {
    return apps.stream()
        .map(
            a ->
                TableRow.of(
                    "/applications/" + a.getId(),
                    a.getStudent().getUser().getFullName(),
                    a.getJob().getTitle(),
                    a.getJob().getCompany().getName(),
                    a.getStatus(),
                    a.getCreatedAt()))
        .toList();
  }

  @GetMapping("/applications")
  public String list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) ApplicationStatus status,
      @RequestParam(required = false) Long jobId,
      @RequestParam(defaultValue = "0") int page,
      Model m) {
    m.addAttribute("statuses", ApplicationStatus.values());
    m.addAttribute("selectedStatus", status);
    m.addAttribute("jobId", jobId);
    return TableSupport.show(
        m,
        "Applications",
        List.of("Student", "Position", "Company", "Status", "Applied at (UTC)"),
        rows(visible(q, status, jobId)),
        page,
        q,
        null,
        access.current().getRole() == Role.STUDENT ? null : "/applications/export");
  }

  @GetMapping("/applications/export")
  public ResponseEntity<String> export(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) ApplicationStatus status,
      @RequestParam(required = false) Long jobId) {
    access.roles(Role.ADMIN, Role.COORDINATOR, Role.COMPANY);
    return TableSupport.csv(
        "applications",
        List.of("Student", "Position", "Company", "Status", "Applied at UTC"),
        rows(visible(q, status, jobId)));
  }

  @GetMapping("/applications/{id}")
  public String detail(@PathVariable Long id, Model m) {
    var a = applications.findById(id).orElseThrow();
    access.view(a);
    m.addAttribute("record", a);
    m.addAttribute("history", history.findByApplicationIdOrderByCreatedAtAsc(id));
    m.addAttribute("interviews", interviews.findByApplicationId(id));
    m.addAttribute("transitions", ApplicationService.next(a.getStatus()));
    m.addAttribute("withdrawable", ApplicationService.withdrawable(a.getStatus()));
    m.addAttribute("canManage", access.manages(access.current(), a.getJob()));
    return "application";
  }

  @PostMapping("/applications/{id}/status")
  @Transactional
  public String status(
      @PathVariable Long id,
      @RequestParam ApplicationStatus status,
      @RequestParam(defaultValue = "") String note,
      RedirectAttributes ra) {
    workflow.transition(id, status, note);
    ra.addFlashAttribute("success", "Recruitment status updated");
    return "redirect:/applications/" + id;
  }

  @PostMapping("/student/applications/{id}/withdraw")
  @Transactional
  public String withdraw(@PathVariable Long id) {
    workflow.withdraw(id);
    return "redirect:/applications/" + id;
  }

  @GetMapping("/applications/{id}/interview")
  public String interview(@PathVariable Long id, Model m) {
    access.manage(applications.findById(id).orElseThrow().getJob());
    m.addAttribute("form", new InterviewForm());
    m.addAttribute("title", "Schedule interview · application #" + id);
    return "forms/interview";
  }

  @PostMapping("/applications/{id}/interview")
  @Transactional
  public String interview(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") InterviewForm f,
      BindingResult errors,
      Model m) {
    access.manage(applications.findById(id).orElseThrow().getJob());
    m.addAttribute("title", "Schedule interview");
    if (errors.hasErrors()) return "forms/interview";
    recruitment.interview(id, f);
    return "redirect:/applications/" + id;
  }

  @PostMapping("/interviews/{id}/status")
  @Transactional
  public String interviewStatus(@PathVariable Long id, @RequestParam InterviewStatus status) {
    recruitment.interviewStatus(id, status);
    return "redirect:/interviews";
  }

  @GetMapping("/interviews")
  public String interviews(Model m) {
    var u = access.current();
    m.addAttribute(
        "interviews",
        interviews.findAll().stream()
            .filter(i -> access.sees(u, i.getApplication()))
            .sorted(Comparator.comparing(Interview::getScheduledAt))
            .toList());
    return "interviews";
  }

  @GetMapping("/applications/{id}/offer")
  public String offer(@PathVariable Long id, Model m) {
    access.manage(applications.findById(id).orElseThrow().getJob());
    m.addAttribute("form", new OfferForm());
    m.addAttribute("title", "Make an offer · application #" + id);
    return "forms/offer";
  }

  @PostMapping("/applications/{id}/offer")
  @Transactional
  public String offer(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") OfferForm f,
      BindingResult errors,
      Model m) {
    access.manage(applications.findById(id).orElseThrow().getJob());
    m.addAttribute("title", "Make an offer");
    if (errors.hasErrors()) return "forms/offer";
    recruitment.offer(id, f);
    return "redirect:/offers";
  }

  @GetMapping("/offers")
  public String offers(Model m) {
    var u = access.current();
    m.addAttribute(
        "offers",
        offers.findAll().stream().filter(o -> access.sees(u, o.getApplication())).toList());
    m.addAttribute("now", java.time.Instant.now());
    return "offers";
  }

  @PostMapping("/student/offers/{id}/respond")
  @Transactional
  public String respond(
      @PathVariable Long id, @RequestParam boolean accept, RedirectAttributes ra) {
    recruitment.respond(id, accept);
    ra.addFlashAttribute("success", accept ? "Offer accepted. Congratulations!" : "Offer declined");
    return "redirect:/offers";
  }
}
