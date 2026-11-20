package edu.campusconnect.config;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {
  @Value("${app.time-zone}")
  private String zone;

  @Bean
  public ZoneId applicationZone() {
    return ZoneId.of(zone);
  }

  @Bean
  public LocalValidatorFactoryBean applicationValidator() {
    var validator = new LocalValidatorFactoryBean();
    validator.setConfigurationInitializer(
        c -> c.clockProvider(() -> Clock.system(applicationZone())));
    return validator;
  }

  @Override
  public org.springframework.validation.Validator getValidator() {
    return applicationValidator();
  }
}
