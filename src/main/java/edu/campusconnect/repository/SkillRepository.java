package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface SkillRepository
    extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {
  Optional<Skill> findByName(String name);
}
