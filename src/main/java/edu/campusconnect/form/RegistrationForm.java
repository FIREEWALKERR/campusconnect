package edu.campusconnect.form;

import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {
  @NotBlank
  @Size(max = 100)
  private String fullName;

  @NotBlank
  @Email
  @Size(max = 190)
  private String email;

  @NotBlank
  @Size(min = 12, max = 64)
  private String password;

  @NotBlank
  @Size(max = 100)
  private String identifier;

  @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
  public boolean isPasswordByteLengthValid() {
    return password == null
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
  }
}
