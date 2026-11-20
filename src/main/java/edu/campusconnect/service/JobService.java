package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.JobForm;
import edu.campusconnect.repository.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {
  private final JobRepository jobs;
  private final CompanyRepository companies;
  private final PlacementDriveRepository drives;
  private final ApplicationRepository applications;
  private final UserRepository users;
  private final AccessService access;
  private final java.time.ZoneId applicationZone;
  private final EventService events;

  @Transactional
  public Job save(Long id, JobForm f) {
    access.roles(Role.ADMIN, Role.COMPANY, Role.COORDINATOR);
    var actor = access.current();
    var j = id == null ? new Job() : jobs.findById(id).orElseThrow();
    if (id != null) access.manage(j);
    var c = companies.findById(f.getCompanyId()).orElseThrow();
    if (actor.getRole() == Role.COMPANY && !c.getUser().getId().equals(actor.getId()))
      throw new org.springframework.security.access.AccessDeniedException("Invalid company");
    if (id != null && !j.getCompany().getId().equals(c.getId()))
      throw new BusinessException("A job's company cannot be changed");
    if (!f.getDeadline().isAfter(f.getOpensAt()))
      throw new BusinessException("Deadline must follow application opening time");
    if (f.getSalaryMax().compareTo(f.getSalaryMin()) < 0)
      throw new BusinessException("Maximum CTC must be at least minimum CTC");
    if (f.getStatus() == JobStatus.PUBLISHED
        && (c.getApproval() != ApprovalState.APPROVED || !c.getUser().isEnabled()))
      throw new BusinessException("Only approved active companies may publish jobs");
    boolean publish = f.getStatus() == JobStatus.PUBLISHED && j.getStatus() != JobStatus.PUBLISHED;
    BeanUtils.copyProperties(
        f, j, "companyId", "driveId", "opensAt", "deadline", "departments", "batches");
    j.setDepartments(new java.util.HashSet<>(f.getDepartments()));
    j.setBatches(new java.util.HashSet<>(f.getBatches()));
    j.setCompany(c);
    j.setDrive(f.getDriveId() == null ? null : drives.findById(f.getDriveId()).orElseThrow());
    j.setOpensAt(f.getOpensAt().atZone(applicationZone).toInstant());
    j.setDeadline(f.getDeadline().atZone(applicationZone).toInstant());
    jobs.save(j);
    events.audit(actor, "Saved job " + j.getStatus(), j.getId());
    if (publish)
      for (var u : users.findByRole(Role.STUDENT))
        events.notify(u, "New opportunity: " + j.getTitle() + " at " + c.getName());
    return j;
  }

  @Transactional
  public void delete(Long id) {
    var j = jobs.findById(id).orElseThrow();
    access.manage(j);
    if (j.getStatus() != JobStatus.DRAFT || !applications.findByJobId(id).isEmpty())
      throw new BusinessException(
          "Only unused draft jobs can be deleted. Close or cancel this job instead.");
    jobs.delete(j);
    events.audit(access.current(), "Deleted draft job", id);
  }
}
