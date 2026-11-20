package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {
  private final ApplicationRepository applications;
  private final StudentProfileRepository students;
  private final JobRepository jobs;
  private final ApplicationStatusHistoryRepository history;
  private final EligibilityService eligibility;
  private final AccessService access;
  private final EventService events;

  public static Set<ApplicationStatus> next(ApplicationStatus s) {
    return switch (s) {
      case APPLIED -> Set.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED);
      case UNDER_REVIEW -> Set.of(ApplicationStatus.SHORTLISTED, ApplicationStatus.REJECTED);
      case SHORTLISTED -> Set.of(ApplicationStatus.REJECTED);
      case INTERVIEW_SCHEDULED -> Set.of(ApplicationStatus.SELECTED, ApplicationStatus.REJECTED);
      case SELECTED -> Set.of(ApplicationStatus.REJECTED);
      default -> Set.of();
    };
  }

  public static boolean withdrawable(ApplicationStatus s) {
    return Set.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.SHORTLISTED,
            ApplicationStatus.INTERVIEW_SCHEDULED,
            ApplicationStatus.SELECTED)
        .contains(s);
  }

  @Transactional
  public Application apply(Long jobId) {
    access.roles(Role.STUDENT);
    var actor = access.current();
    var s = students.findByUserId(actor.getId()).orElseThrow();
    var j = jobs.findById(jobId).orElseThrow();
    var reasons = eligibility.reasons(s, j);
    if (!reasons.isEmpty()) throw new BusinessException(String.join("; ", reasons));
    var a = new Application();
    a.setStudent(s);
    a.setJob(j);
    applications.saveAndFlush(a);
    record(a, null, ApplicationStatus.APPLIED, actor, "Application submitted");
    return a;
  }

  @Transactional
  public void transition(Long id, ApplicationStatus target, String note) {
    var a = applications.findById(id).orElseThrow();
    access.manage(a.getJob());
    if (!next(a.getStatus()).contains(target))
      throw new BusinessException("Invalid transition from " + a.getStatus() + " to " + target);
    change(a, target, note);
  }

  @Transactional
  public void withdraw(Long id) {
    var a = applications.findById(id).orElseThrow();
    if (!a.getStudent().getUser().getId().equals(access.current().getId()))
      throw new org.springframework.security.access.AccessDeniedException("Not your application");
    if (!withdrawable(a.getStatus()))
      throw new BusinessException("This application cannot be withdrawn");
    change(a, ApplicationStatus.WITHDRAWN, "Withdrawn by student");
  }

  void change(Application a, ApplicationStatus target, String note) {
    var old = a.getStatus();
    a.setStatus(target);
    record(a, old, target, access.current(), note);
  }

  private void record(
      Application a, ApplicationStatus old, ApplicationStatus target, User actor, String note) {
    var h = new ApplicationStatusHistory();
    h.setApplication(a);
    h.setPreviousStatus(old);
    h.setStatus(target);
    h.setActor(actor);
    h.setNote(note == null ? "" : note.substring(0, Math.min(1000, note.length())));
    history.save(h);
    events.notify(a.getStudent().getUser(), a.getJob().getTitle() + ": " + target);
    events.audit(actor, "Application status " + target, a.getId());
  }
}
