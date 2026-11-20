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
public class AnnouncementForm {
  @NotBlank
  @Size(max = 150)
  private String title;

  @NotBlank
  @Size(max = 5000)
  private String body;

  @NotNull
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime publishedAt;
}
