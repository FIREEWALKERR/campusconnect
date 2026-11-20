package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final UserRepository users;
  private final StudentProfileRepository students;
  private final CompanyRepository companies;
  private final SkillRepository skills;
  private final PasswordEncoder encoder;
  private final AccessService access;
  private final EventService events;

  @Transactional
  public User register(RegistrationForm f, Role role) {
    if (role != Role.STUDENT && role != Role.COMPANY)
      throw new BusinessException("Public registration is for students and companies only");
    String email = f.getEmail().trim().toLowerCase(Locale.ROOT);
    if (users.existsByEmail(email)) throw new BusinessException("This email is already registered");
    String identifier = f.getIdentifier().trim().toUpperCase(Locale.ROOT);
    if (role == Role.STUDENT && students.existsByRegistrationNumber(identifier))
      throw new BusinessException("Registration number already exists");
    var u = new User();
    u.setEmail(email);
    u.setFullName(f.getFullName().trim());
    u.setPassword(encoder.encode(f.getPassword()));
    u.setRole(role);
    users.saveAndFlush(u);
    if (role == Role.STUDENT) {
      var s = new StudentProfile();
      s.setUser(u);
      s.setRegistrationNumber(identifier);
      students.save(s);
    } else {
      var c = new Company();
      c.setUser(u);
      c.setName(f.getIdentifier().trim());
      companies.save(c);
    }
    events.notify(u, "Welcome to CampusConnect. Complete your profile to get started.");
    return u;
  }

  @Transactional
  public void profile(Long id, ProfileForm f) {
    var s = students.findById(id).orElseThrow();
    var u = access.current();
    if (!s.getUser().getId().equals(u.getId())) access.roles(Role.ADMIN);
    BeanUtils.copyProperties(f, s, "fullName", "skills");
    s.getUser().setFullName(f.getFullName());
    s.getSkills().clear();
    if (f.getSkills() != null)
      for (String part : f.getSkills().split(",")) {
        String name = part.trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) continue;
        if (name.length() > 80)
          throw new BusinessException("Each skill must be at most 80 characters");
        s.getSkills()
            .add(
                skills
                    .findByName(name)
                    .orElseGet(
                        () -> {
                          var skill = new Skill();
                          skill.setName(name);
                          return skills.save(skill);
                        }));
      }
    events.audit(u, "Updated student profile", id);
  }

  @Transactional
  public void company(CompanyForm f) {
    access.roles(Role.COMPANY);
    var c = companies.findByUserId(access.current().getId()).orElseThrow();
    BeanUtils.copyProperties(f, c);
    events.audit(access.current(), "Updated company profile", c.getId());
  }

  @Transactional
  public void approve(Long id, ApprovalState state) {
    access.roles(Role.ADMIN);
    var c = companies.findById(id).orElseThrow();
    c.setApproval(state);
    events.notify(c.getUser(), "Company approval status: " + state);
    events.audit(access.current(), "Company approval " + state, id);
  }

  @Transactional
  public void enabled(Long id, boolean enabled) {
    access.roles(Role.ADMIN);
    var u = users.findById(id).orElseThrow();
    if (u.getRole() == Role.ADMIN)
      throw new BusinessException("Administrator accounts cannot be disabled here");
    u.setEnabled(enabled);
    events.audit(access.current(), "Account enabled=" + enabled, id);
  }

  @Transactional
  public void password(PasswordForm f) {
    var u = access.current();
    if (!encoder.matches(f.getCurrentPassword(), u.getPassword()))
      throw new BusinessException("Current password is incorrect");
    u.setPassword(encoder.encode(f.getNewPassword()));
    events.audit(u, "Changed password", u.getId());
  }
}
