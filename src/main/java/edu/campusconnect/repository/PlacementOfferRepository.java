package edu.campusconnect.repository;

import edu.campusconnect.entity.*;
import edu.campusconnect.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PlacementOfferRepository
    extends JpaRepository<PlacementOffer, Long>, JpaSpecificationExecutor<PlacementOffer> {
  boolean existsByApplicationId(Long id);
}
