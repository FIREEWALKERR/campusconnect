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
public class AuditLog extends BaseEntity {
  @ManyToOne(optional = false)
  private User actor;

  private String action;
  private String target;
}
