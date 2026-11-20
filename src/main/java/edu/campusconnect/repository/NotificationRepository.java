package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface NotificationRepository
    extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long id);
}
