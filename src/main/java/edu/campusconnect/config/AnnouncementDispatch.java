package edu.campusconnect.config;

import edu.campusconnect.repository.AnnouncementRepository;
import edu.campusconnect.repository.UserRepository;
import edu.campusconnect.service.EventService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** Includes publications whose scheduled time passed during application downtime. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "app.announcement-dispatch-enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class AnnouncementDispatch {
  private final AnnouncementRepository announcements;
  private final UserRepository users;
  private final EventService events;

  @Scheduled(fixedDelay = 60000, initialDelay = 60000)
  @Transactional
  public void dispatch() {
    var now = Instant.now();
    for (var announcement :
        announcements.findByPublishedAtLessThanEqualOrderByPublishedAtDesc(now)) {
      if (announcement.getNotifiedAt() != null) continue;
      for (var user : users.findAll())
        if (user.isEnabled()) events.notify(user, "Announcement: " + announcement.getTitle());
      announcement.setNotifiedAt(now);
    }
  }
}
