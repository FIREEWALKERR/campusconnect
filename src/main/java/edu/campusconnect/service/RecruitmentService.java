package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitmentService {
  private final ApplicationRepository applications;
  private final InterviewRepository interviews;
  private final PlacementOfferRepository offers;
  private final ApplicationService workflow;
  private final AccessService access;

  @org.springframework.beans.factory.annotation.Value("${app.currency}")
  private String currency;

  private final java.time.ZoneId applicationZone;
  private final EventService events;
  private final Clock clock;

  @Transactional
  public void interview(Long applicationId, InterviewForm f) {
    var a = applications.findById(applicationId).orElseThrow();
    access.manage(a.getJob());
    if (a.getStatus() != ApplicationStatus.SHORTLISTED
        && a.getStatus() != ApplicationStatus.INTERVIEW_SCHEDULED)
      throw new BusinessException("Shortlist the applicant before scheduling interviews");
    var instant = f.getScheduledAt().atZone(applicationZone).toInstant();
    if (!instant.isAfter(clock.instant()))
      throw new BusinessException("Interview must be in the future");
    if (interviews.findByApplicationId(applicationId).stream()
        .anyMatch(
            i ->
                i.getRoundNumber() == f.getRoundNumber()
                    && i.getStatus() != InterviewStatus.CANCELLED))
      throw new BusinessException("This round is already scheduled");
    var i = new Interview();
    BeanUtils.copyProperties(f, i, "scheduledAt");
    i.setApplication(a);
    i.setScheduledAt(instant);
    interviews.save(i);
    if (a.getStatus() == ApplicationStatus.SHORTLISTED)
      workflow.change(a, ApplicationStatus.INTERVIEW_SCHEDULED, "Interview scheduled");
    events.notify(
        a.getStudent().getUser(),
        "Interview round "
            + i.getRoundNumber()
            + " on "
            + f.getScheduledAt()
            + " "
            + applicationZone
            + ": "
            + i.getLocation());
    events.audit(access.current(), "Scheduled interview", i.getId());
  }

  @Transactional
  public void interviewStatus(Long id, InterviewStatus status) {
    var i = interviews.findById(id).orElseThrow();
    access.manage(i.getApplication().getJob());
    if (i.getStatus() != InterviewStatus.SCHEDULED || status == InterviewStatus.SCHEDULED)
      throw new BusinessException("Only scheduled interviews can be completed or cancelled");
    i.setStatus(status);
    events.notify(
        i.getApplication().getStudent().getUser(),
        "Interview round " + i.getRoundNumber() + ": " + status);
    events.audit(access.current(), "Interview " + status, id);
  }

  @Transactional
  public void offer(Long applicationId, OfferForm f) {
    var a = applications.findById(applicationId).orElseThrow();
    access.manage(a.getJob());
    if (a.getStatus() != ApplicationStatus.SELECTED)
      throw new BusinessException("Select the applicant before making an offer");
    if (offers.existsByApplicationId(applicationId))
      throw new BusinessException("An offer already exists for this application");
    var deadline = f.getAcceptanceDeadline().atZone(applicationZone).toInstant();
    if (!deadline.isAfter(clock.instant()))
      throw new BusinessException("Offer deadline must be in the future");
    var o = new PlacementOffer();
    o.setApplication(a);
    o.setSalary(f.getSalary());
    o.setOfferDate(clock.instant());
    o.setAcceptanceDeadline(deadline);
    offers.saveAndFlush(o);
    workflow.change(
        a, ApplicationStatus.OFFERED, "Offer issued; annual CTC " + currency + " " + f.getSalary());
  }

  @Transactional
  public void respond(Long id, boolean accept) {
    access.roles(Role.STUDENT);
    var o = offers.findById(id).orElseThrow();
    var a = o.getApplication();
    if (!a.getStudent().getUser().getId().equals(access.current().getId()))
      throw new org.springframework.security.access.AccessDeniedException("Not your offer");
    if (o.getStatus() != OfferStatus.PENDING
        || a.getStatus() != ApplicationStatus.OFFERED
        || !clock.instant().isBefore(o.getAcceptanceDeadline()))
      throw new BusinessException("This offer is no longer open for response");
    o.setStatus(accept ? OfferStatus.ACCEPTED : OfferStatus.DECLINED);
    o.setRespondedAt(clock.instant());
    workflow.change(
        a,
        accept ? ApplicationStatus.OFFER_ACCEPTED : ApplicationStatus.OFFER_DECLINED,
        "Student responded to offer");
  }
}
