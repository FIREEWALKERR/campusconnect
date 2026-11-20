package edu.campusconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/** College-maintained reference data, rather than a fixed list in the code. */
@Entity
@Getter
@Setter
public class Department extends BaseEntity {
  @Column(nullable = false, unique = true, length = 40)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Override
  public String toString() {
    return code;
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || (other instanceof Department d && getId() != null && getId().equals(d.getId()));
  }

  @Override
  public int hashCode() {
    return Department.class.hashCode();
  }
}
