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
public class Company extends BaseEntity {
  @OneToOne(optional = false)
  @JoinColumn(nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  private String name;

  private String website;
  private String location;

  @Column(length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  private ApprovalState approval = ApprovalState.PENDING;
}
