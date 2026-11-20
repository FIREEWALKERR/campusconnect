package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {
  private final NotificationRepository notifications;
  private final AuditLogRepository audit;

  public void notify(User user, String message) {
    var n = new Notification();
    n.setUser(user);
    n.setMessage(message);
    notifications.save(n);
  }

  public void audit(User user, String action, Object target) {
    var a = new AuditLog();
    a.setActor(user);
    a.setAction(action);
    a.setTarget(String.valueOf(target));
    audit.save(a);
  }
}
