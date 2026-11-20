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
public class ApplicationStatusHistory extends BaseEntity {
  @ManyToOne(optional = false)
  private Application application;

  @ManyToOne(optional = false)
  private User actor;

  @Enumerated(EnumType.STRING)
  private ApplicationStatus previousStatus;

  @Enumerated(EnumType.STRING)
  private ApplicationStatus status;

  @Column(length = 1000)
  private String note;
}
