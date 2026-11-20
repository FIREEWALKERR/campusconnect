package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessService {
  private final UserRepository users;

  public User current() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) throw new AccessDeniedException("Please sign in");
    var u =
        users
            .findByEmail(a.getName())
            .orElseThrow(() -> new AccessDeniedException("Please sign in"));
    if (!u.isEnabled()) throw new AccessDeniedException("Account disabled");
    return u;
  }

  public boolean staff(User u) {
    return u.getRole() == Role.ADMIN || u.getRole() == Role.COORDINATOR;
  }

  public void roles(Role... roles) {
    var u = current();
    for (var r : roles) if (u.getRole() == r) return;
    throw new AccessDeniedException("You do not have permission for this action");
  }

  public boolean manages(User u, Job j) {
    return staff(u)
        || (u.getRole() == Role.COMPANY && j.getCompany().getUser().getId().equals(u.getId()));
  }

  public void manage(Job job) {
    if (!manages(current(), job))
      throw new AccessDeniedException("This recruitment record belongs to another company");
  }

  public boolean sees(User u, Application a) {
    return manages(u, a.getJob()) || a.getStudent().getUser().getId().equals(u.getId());
  }

  public void view(Application a) {
    if (!sees(current(), a)) throw new AccessDeniedException("This application is private");
  }
}
