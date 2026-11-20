package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.repository.ApplicationRepository;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EligibilityService {
  private final ApplicationRepository applications;
  private final Clock clock;

  public List<String> reasons(StudentProfile s, Job j) {
    var reasons = new ArrayList<String>();
    var now = clock.instant();
    if (!s.getUser().isEnabled()) reasons.add("Your account is disabled");
    if (j.getStatus() != JobStatus.PUBLISHED
        || j.getCompany().getApproval() != ApprovalState.APPROVED
        || !j.getCompany().getUser().isEnabled())
      reasons.add("This job is not published by an approved active company");
    if (j.getOpensAt() == null || now.isBefore(j.getOpensAt()))
      reasons.add("Applications have not opened yet");
    if (j.getDeadline() == null || !now.isBefore(j.getDeadline()))
      reasons.add("The application deadline has passed");
    if (s.getCgpa() == null || s.getCgpa().compareTo(j.getMinimumCgpa()) < 0)
      reasons.add("Minimum CGPA: " + j.getMinimumCgpa());
    if (s.getDepartment() == null || !j.getDepartments().contains(s.getDepartment()))
      reasons.add("Your department is not eligible");
    if (s.getGraduationYear() == null || !j.getBatches().contains(s.getGraduationYear()))
      reasons.add("Your graduation batch is not eligible");
    if (s.getBacklogs() == null || s.getBacklogs() > j.getMaximumBacklogs())
      reasons.add("Maximum active backlogs: " + j.getMaximumBacklogs());
    if (applications.existsByStudentIdAndJobId(s.getId(), j.getId()))
      reasons.add("You have already applied");
    return reasons;
  }
}
