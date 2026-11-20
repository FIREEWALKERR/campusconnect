package edu.campusconnect.controller;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import edu.campusconnect.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PublicController {
  private final AccountService accounts;
  private final StudentProfileRepository students;
  private final CompanyRepository companies;
  private final JobRepository jobs;
  private final PlacementOfferRepository offers;

  @GetMapping("/")
  @Transactional(readOnly = true)
  public String home(Model m) {
    m.addAttribute("studentCount", students.count());
    m.addAttribute(
        "companyCount",
        companies.findAll().stream()
            .filter(c -> c.getApproval() == ApprovalState.APPROVED)
            .count());
    m.addAttribute(
        "placedCount",
        offers.findAll().stream()
            .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
            .map(o -> o.getApplication().getStudent().getId())
            .distinct()
            .count());
    m.addAttribute("jobs", jobs.findAll().stream().filter(Job::isOpen).limit(4).toList());
    m.addAttribute(
        "companies",
        companies.findAll().stream()
            .filter(c -> c.getApproval() == ApprovalState.APPROVED)
            .limit(5)
            .toList());
    return "home";
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/about")
  public String about() {
    return "about";
  }

  @GetMapping("/register/{kind:student|company}")
  public String register(@PathVariable String kind, Model m) {
    m.addAttribute("form", new RegistrationForm());
    m.addAttribute("kind", kind);
    return "register";
  }

  @PostMapping("/register/{kind:student|company}")
  public String register(
      @PathVariable String kind,
      @Valid @ModelAttribute("form") RegistrationForm f,
      BindingResult errors,
      Model m) {
    m.addAttribute("kind", kind);
    if (errors.hasErrors()) return "register";
    try {
      accounts.register(f, kind.equals("student") ? Role.STUDENT : Role.COMPANY);
    } catch (BusinessException e) {
      errors.reject("registration", e.getMessage());
      return "register";
    }
    return "redirect:/login?registered";
  }
}
