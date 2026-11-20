package edu.campusconnect.service;

import edu.campusconnect.entity.User;
import edu.campusconnect.enums.Role;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.StaffAccountForm;
import edu.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffAccountService {
  private final AccessService access;
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final EventService events;

  @Transactional
  public void create(StaffAccountForm form) {
    access.roles(Role.ADMIN);
    String email = form.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
    if (users.existsByEmail(email)) throw new BusinessException("This email is already registered");
    var user = new User();
    user.setFullName(form.getFullName().trim());
    user.setEmail(email);
    user.setPassword(encoder.encode(form.getPassword()));
    user.setRole(Role.COORDINATOR);
    users.save(user);
    events.audit(access.current(), "Created placement coordinator", user.getId());
  }
}
