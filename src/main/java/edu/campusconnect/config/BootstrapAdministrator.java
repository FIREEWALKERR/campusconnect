package edu.campusconnect.config;

import edu.campusconnect.entity.User;
import edu.campusconnect.enums.Role;
import edu.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit one-time bootstrap for an empty non-demo installation. */
@Component
@Profile("bootstrap & !dev")
@RequiredArgsConstructor
public class BootstrapAdministrator implements CommandLineRunner {
  private final UserRepository users;
  private final PasswordEncoder encoder;

  @Value("${BOOTSTRAP_EMAIL:}")
  private String email;

  @Value("${BOOTSTRAP_PASSWORD:}")
  private String password;

  @Override
  @Transactional
  public void run(String... args) {
    if (!users.findByRole(Role.ADMIN).isEmpty()) return;
    if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        || password.length() < 12
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      throw new IllegalStateException(
          "Set a valid BOOTSTRAP_EMAIL and a BOOTSTRAP_PASSWORD of at least 12 characters and at"
              + " most 72 UTF-8 bytes.");
    }
    var user = new User();
    user.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
    user.setFullName("Placement Administrator");
    user.setPassword(encoder.encode(password));
    user.setRole(Role.ADMIN);
    users.save(user);
  }
}
