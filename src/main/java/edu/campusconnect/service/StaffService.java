package edu.campusconnect.service;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import edu.campusconnect.exception.BusinessException;
import edu.campusconnect.form.*;
import edu.campusconnect.repository.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffService {
  private final PlacementDriveRepository drives;
  private final AnnouncementRepository announcements;
  private final UserRepository users;
  private final AccessService access;
  private final java.time.ZoneId applicationZone;
  private final EventService events;

  @Transactional
  public void drive(Long id, DriveForm f) {
    access.roles(Role.ADMIN, Role.COORDINATOR);
    if (f.getEndDate().isBefore(f.getStartDate()))
      throw new BusinessException("Drive end date must follow start date");
    var d = id == null ? new PlacementDrive() : drives.findById(id).orElseThrow();
    BeanUtils.copyProperties(f, d);
    drives.save(d);
    events.audit(access.current(), "Saved placement drive", d.getId());
  }

  @Transactional
  public void announcement(Long id, AnnouncementForm f) {
    access.roles(Role.ADMIN, Role.COORDINATOR);
    var a = id == null ? new Announcement() : announcements.findById(id).orElseThrow();
    BeanUtils.copyProperties(f, a, "publishedAt");
    a.setPublishedAt(f.getPublishedAt().atZone(applicationZone).toInstant());
    a.setAuthor(access.current());
    announcements.save(a);
    events.audit(access.current(), "Saved announcement", a.getId());
    a.setNotifiedAt(null);
    if (!a.getPublishedAt().isAfter(Instant.now())) {
      for (var u : users.findAll())
        if (u.isEnabled()) events.notify(u, "Announcement: " + a.getTitle());
      a.setNotifiedAt(Instant.now());
    }
  }

  @Transactional
  public void deleteAnnouncement(Long id) {
    access.roles(Role.ADMIN, Role.COORDINATOR);
    announcements.deleteById(id);
    events.audit(access.current(), "Deleted announcement", id);
  }
}
