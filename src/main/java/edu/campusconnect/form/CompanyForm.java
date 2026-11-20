package edu.campusconnect.form;

import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyForm {
  @NotBlank
  @Size(max = 150)
  private String name;

  @Size(max = 250)
  @Pattern(regexp = "(^$|https?://.*)")
  private String website;

  @NotBlank
  @Size(max = 150)
  private String location;

  @NotBlank
  @Size(max = 4000)
  private String description;
}
