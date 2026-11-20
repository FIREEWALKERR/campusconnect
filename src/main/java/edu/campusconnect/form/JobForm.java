package edu.campusconnect.form;

import edu.campusconnect.entity.Department;
import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class JobForm {
  @NotNull private Long companyId;
  private Long driveId;

  @NotBlank
  @Size(max = 150)
  private String title;

  @NotBlank
  @Size(max = 8000)
  private String description;

  @NotBlank
  @Size(max = 2000)
  private String requiredSkills;

  @NotBlank
  @Size(max = 100)
  private String category;

  @NotBlank
  @Size(max = 150)
  private String location;

  @NotNull private EmploymentType employmentType;

  @NotNull
  @DecimalMin("0")
  @Digits(integer = 12, fraction = 2)
  private BigDecimal salaryMin;

  @NotNull
  @DecimalMin("0")
  @Digits(integer = 12, fraction = 2)
  private BigDecimal salaryMax;

  @NotNull
  @DecimalMin("0")
  @DecimalMax("10")
  private BigDecimal minimumCgpa;

  @Min(0)
  @Max(50)
  private int maximumBacklogs;

  @NotEmpty private Set<Department> departments = new HashSet<>();
  @NotEmpty private Set<@Min(1) @Max(9999) Integer> batches = new HashSet<>();

  @NotNull
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime opensAt;

  @NotNull
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime deadline;

  @Min(1)
  @Max(10000)
  private int vacancies = 1;

  @NotNull private JobStatus status = JobStatus.DRAFT;
}
