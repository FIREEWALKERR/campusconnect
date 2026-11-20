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
@Table(indexes = @Index(columnList = "department_id,graduationYear"))
public class StudentProfile extends BaseEntity {
  @OneToOne(optional = false)
  @JoinColumn(unique = true, nullable = false)
  private User user;

  @Column(nullable = false, unique = true)
  private String registrationNumber;

  private String phone;

  @ManyToOne private Department department;

  private String degree;
  private Integer graduationYear;

  @Column(precision = 4, scale = 2)
  private BigDecimal cgpa;

  private Integer backlogs;

  @Lob
  @Column(length = 4000)
  private String academicHistory;

  @Lob
  @Column(length = 4000)
  private String certifications;

  @Lob
  @Column(length = 4000)
  private String projects;

  @Lob
  @Column(length = 4000)
  private String achievements;

  private String resumeKey;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "student_skill",
      joinColumns = @JoinColumn(name = "student_id"),
      inverseJoinColumns = @JoinColumn(name = "skill_id"))
  private Set<Skill> skills = new HashSet<>();

  public int getCompletion() {
    int n = 0;
    if (phone != null && !phone.isBlank()) n++;
    if (department != null) n++;
    if (degree != null && !degree.isBlank()) n++;
    if (graduationYear != null) n++;
    if (cgpa != null) n++;
    if (backlogs != null) n++;
    if (!skills.isEmpty()) n++;
    if (resumeKey != null) n++;
    return n * 100 / 8;
  }
}
