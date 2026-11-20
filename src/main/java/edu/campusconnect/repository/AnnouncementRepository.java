package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AnnouncementRepository
    extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {
  List<Announcement> findByPublishedAtLessThanEqualOrderByPublishedAtDesc(Instant time);
}
