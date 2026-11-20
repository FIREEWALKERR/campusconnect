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
@Table(indexes = @Index(columnList = "user_id,readFlag"))
public class Notification extends BaseEntity {
  @ManyToOne(optional = false)
  private User user;

  @Column(length = 1000)
  private String message;

  private boolean readFlag = false;
}
