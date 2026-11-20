package edu.campusconnect.config;

import edu.campusconnect.entity.Department;
import edu.campusconnect.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentConverter implements Converter<String, Department> {
  private final DepartmentRepository departments;

  @Override
  public Department convert(String code) {
    if (code.isBlank()) return null;
    return departments
        .findByCode(code.trim().toUpperCase(java.util.Locale.ROOT))
        .orElseThrow(() -> new IllegalArgumentException("Choose a configured department"));
  }
}
