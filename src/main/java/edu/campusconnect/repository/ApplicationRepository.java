package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ApplicationRepository
    extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
  boolean existsByStudentIdAndJobId(Long studentId, Long jobId);

  List<Application> findByStudentId(Long id);

  List<Application> findByJobCompanyId(Long id);

  List<Application> findByJobId(Long id);
}
