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
public class Announcement extends BaseEntity {
  private String title;

  @Column(length = 5000)
  private String body;

  @ManyToOne(optional = false)
  private User author;

  private Instant publishedAt;
  private Instant notifiedAt;
}
