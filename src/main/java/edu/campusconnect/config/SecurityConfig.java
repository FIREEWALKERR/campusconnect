package edu.campusconnect.config;

import edu.campusconnect.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email -> {
      var u =
          users
              .findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
              .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
      return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
          .password(u.getPassword())
          .roles(u.getRole().name())
          .disabled(!u.isEnabled())
          .build();
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/about",
                        "/login",
                        "/register/**",
                        "/css/**",
                        "/js/**",
                        "/webjars/**",
                        "/error")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/staff/**")
                    .hasAnyRole("ADMIN", "COORDINATOR")
                    .requestMatchers("/student/**")
                    .hasRole("STUDENT")
                    .requestMatchers("/company/**")
                    .hasRole("COMPANY")
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
        .logout(
            l ->
                l.logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()).maximumSessions(1));
    return http.build();
  }

  @Bean
  java.time.Clock clock() {
    return java.time.Clock.systemUTC();
  }
}
