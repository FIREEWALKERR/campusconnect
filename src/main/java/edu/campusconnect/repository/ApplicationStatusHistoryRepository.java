package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ApplicationStatusHistoryRepository
    extends JpaRepository<ApplicationStatusHistory, Long>,
        JpaSpecificationExecutor<ApplicationStatusHistory> {
  List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAsc(Long id);
}
