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
public class InterviewForm {
  @NotNull
  @Future
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime scheduledAt;

  @Min(1)
  @Max(20)
  private int roundNumber = 1;

  @NotBlank
  @Size(max = 100)
  private String type;

  @NotBlank
  @Size(max = 250)
  private String location;

  @Size(max = 2000)
  private String instructions;
}
