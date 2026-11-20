package edu.campusconnect.form;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentForm {
  @NotBlank
  @Pattern(regexp = "[A-Z0-9_-]{1,40}")
  private String code;

  @NotBlank
  @Size(max = 150)
  private String name;
}
