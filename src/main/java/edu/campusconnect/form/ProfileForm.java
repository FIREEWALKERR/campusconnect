package edu.campusconnect.form;

import edu.campusconnect.entity.Department;
import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {
  @NotBlank
  @Size(max = 100)
  private String fullName;

  @NotBlank
  @Pattern(regexp = "[+0-9 ()-]{7,20}")
  private String phone;

  @NotNull private Department department;

  @NotBlank
  @Size(max = 80)
  private String degree;

  @NotNull
  @Min(1)
  @Max(9999)
  private Integer graduationYear;

  @NotNull
  @DecimalMin("0.0")
  @DecimalMax("10.0")
  private BigDecimal cgpa;

  @NotNull
  @Min(0)
  @Max(50)
  private Integer backlogs;

  @Size(max = 4000)
  private String academicHistory;

  @Size(max = 4000)
  private String certifications;

  @Size(max = 4000)
  private String projects;

  @Size(max = 4000)
  private String achievements;

  @Size(max = 1000)
  private String skills;
}
