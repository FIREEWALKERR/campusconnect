package edu.campusconnect.form;

import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordForm {
  @NotBlank private String currentPassword;

  @NotBlank
  @Size(min = 12, max = 64)
  private String newPassword;

  @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
  public boolean isPasswordByteLengthValid() {
    return newPassword == null
        || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
  }
}
