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
public class PlacementDrive extends BaseEntity {
  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String academicYear;

  private int batch;
  private LocalDate startDate;
  private LocalDate endDate;
  private String venue;
}
