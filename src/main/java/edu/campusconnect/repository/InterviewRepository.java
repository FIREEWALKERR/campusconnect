package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface InterviewRepository
    extends JpaRepository<Interview, Long>, JpaSpecificationExecutor<Interview> {
  List<Interview> findByApplicationId(Long id);
}
