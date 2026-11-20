package edu.campusconnect.form;

import edu.campusconnect.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class OfferForm {
  @NotNull
  @DecimalMin("0.01")
  @Digits(integer = 12, fraction = 2)
  private BigDecimal salary;

  @NotNull
  @Future
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime acceptanceDeadline;
}
