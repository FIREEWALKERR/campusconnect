package edu.campusconnect.entity;

import edu.campusconnect.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "job_application",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "job_id"}),
    indexes = {@Index(columnList = "status"), @Index(columnList = "job_id")})
public class Application extends BaseEntity {
  @ManyToOne(optional = false)
  private StudentProfile student;

  @ManyToOne(optional = false)
  private Job job;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApplicationStatus status = ApplicationStatus.APPLIED;
}
