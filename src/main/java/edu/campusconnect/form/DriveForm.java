package edu.campusconnect.form;

import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class DriveForm {
  @NotBlank
  @Size(max = 150)
  private String name;

  @NotBlank
  @Pattern(regexp = "[0-9]{4}-[0-9]{4}")
  private String academicYear;

  @Min(1)
  @Max(9999)
  private int batch;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  @NotBlank
  @Size(max = 200)
  private String venue;
}
