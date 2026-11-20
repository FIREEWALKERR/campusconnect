package edu.campusconnect.entity;

import edu.campusconnect.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PlacementOffer extends BaseEntity {
  @OneToOne(optional = false)
  @JoinColumn(nullable = false, unique = true)
  private Application application;

  @Column(precision = 14, scale = 2, nullable = false)
  private BigDecimal salary;

  private Instant offerDate;
  private Instant acceptanceDeadline;
  private Instant respondedAt;

  @Enumerated(EnumType.STRING)
  private OfferStatus status = OfferStatus.PENDING;
}
