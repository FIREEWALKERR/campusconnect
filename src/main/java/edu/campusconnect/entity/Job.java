package edu.campusconnect.entity;

import edu.campusconnect.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "job",
    indexes = {@Index(columnList = "status,deadline"), @Index(columnList = "company_id")})
public class Job extends BaseEntity {
  @ManyToOne(optional = false)
  private Company company;

  @ManyToOne private PlacementDrive drive;

  @Column(nullable = false)
  private String title;

  @Column(length = 8000, nullable = false)
  private String description;

  @Column(length = 2000)
  private String requiredSkills;

  private String category;
  private String location;

  @Enumerated(EnumType.STRING)
  private EmploymentType employmentType;

  @Column(precision = 14, scale = 2)
  private BigDecimal salaryMin;

  @Column(precision = 14, scale = 2)
  private BigDecimal salaryMax;

  @Column(precision = 4, scale = 2)
  private BigDecimal minimumCgpa;

  private int maximumBacklogs;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "job_department",
      joinColumns = @JoinColumn(name = "job_id"),
      inverseJoinColumns = @JoinColumn(name = "department_id"))
  private Set<Department> departments = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "job_batch")
  private Set<Integer> batches = new HashSet<>();

  private Instant opensAt;
  private Instant deadline;
  private int vacancies;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JobStatus status = JobStatus.DRAFT;

  public boolean isOpen() {
    Instant now = Instant.now();
    return status == JobStatus.PUBLISHED
        && company.getApproval() == ApprovalState.APPROVED
        && company.getUser().isEnabled()
        && !now.isBefore(opensAt)
        && now.isBefore(deadline);
  }
}
