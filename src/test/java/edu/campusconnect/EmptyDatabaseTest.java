package edu.campusconnect;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class EmptyDatabaseTest {
  @Test
  void emptyDatabaseRendersAndRegistrationWorksWithoutDemoProfile() throws Exception {
    try (var context =
        new SpringApplicationBuilder(CampusConnectApplication.class)
            .profiles("test")
            .run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:empty;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
      assertEquals(0, context.getBean(UserRepository.class).count());
      var mvc =
          MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
              .apply(springSecurity())
              .build();
      mvc.perform(get("/")).andExpect(status().isOk());
      mvc.perform(get("/login")).andExpect(status().isOk());
      mvc.perform(
              post("/register/student")
                  .with(csrf())
                  .param("fullName", "First Student")
                  .param("email", "first@example.test")
                  .param("password", "SecureTest!2026")
                  .param("identifier", "FIRST001"))
          .andExpect(redirectedUrl("/login?registered"));
      assertEquals(1, context.getBean(UserRepository.class).count());
    }
  }
}
