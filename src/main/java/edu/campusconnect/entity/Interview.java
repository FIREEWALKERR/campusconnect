package edu.campusconnect.entity;

import edu.campusconnect.enums.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Interview extends BaseEntity {
  @ManyToOne(optional = false)
  private Application application;

  private Instant scheduledAt;
  private int roundNumber;
  private String type;
  private String location;

  @Column(length = 2000)
  private String instructions;

  @Enumerated(EnumType.STRING)
  private InterviewStatus status = InterviewStatus.SCHEDULED;
}
