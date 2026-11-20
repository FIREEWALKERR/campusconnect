package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface StudentProfileRepository
    extends JpaRepository<StudentProfile, Long>, JpaSpecificationExecutor<StudentProfile> {
  Optional<StudentProfile> findByUserId(Long id);

  boolean existsByRegistrationNumber(String registrationNumber);
}
