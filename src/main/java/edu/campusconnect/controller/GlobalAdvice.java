package edu.campusconnect.controller;

import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalAdvice {
  private final AccessService access;
  private final java.time.ZoneId applicationZone;

  @org.springframework.beans.factory.annotation.Value("${app.currency}")
  private String currency;

  @org.springframework.beans.factory.annotation.Value("${app.contact-email}")
  private String contactEmail;

  @org.springframework.beans.factory.annotation.Value("${spring.servlet.multipart.max-file-size}")
  private String resumeLimit;

  private final edu.campusconnect.repository.DepartmentRepository departmentRepository;

  @ModelAttribute
  public void common(Model m) {
    m.addAttribute("timeZone", applicationZone.getId());
    m.addAttribute("currency", currency);
    m.addAttribute("contactEmail", contactEmail);
    m.addAttribute("resumeLimit", resumeLimit);
    m.addAttribute("departments", departmentRepository.findAll());
    m.addAttribute("jobStatuses", JobStatus.values());
    m.addAttribute("employmentTypes", EmploymentType.values());
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a != null && !a.getName().equals("anonymousUser")) {
      try {
        var u = access.current();
        m.addAttribute("me", u);
        m.addAttribute("staff", access.staff(u));
        m.addAttribute("manager", access.staff(u) || u.getRole() == Role.COMPANY);
      } catch (AccessDeniedException ignored) {
      }
    }
  }

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String business(BusinessException e, Model m) {
    m.addAttribute("message", e.getMessage());
    return "error";
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String forbidden(Model m) {
    m.addAttribute("message", "You do not have permission to access this record.");
    return "error";
  }

  @ExceptionHandler(java.util.NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String missing(Model m) {
    m.addAttribute("message", "This record could not be found.");
    return "error";
  }

  @ExceptionHandler({
    org.springframework.dao.DataIntegrityViolationException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  @ResponseStatus(HttpStatus.CONFLICT)
  public String conflict(Model m) {
    m.addAttribute(
        "message",
        "This record already exists or was changed by another user. Reload the page and try"
            + " again.");
    return "error";
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String upload(Model m) {
    m.addAttribute(
        "message", "The resume exceeds the configured upload limit of " + resumeLimit + ".");
    return "error";
  }
}
