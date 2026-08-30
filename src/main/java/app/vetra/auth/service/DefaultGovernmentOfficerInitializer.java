package app.vetra.auth.service;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes and ensures authorized Government Officer and Administrator accounts exist
 * on application startup with valid encoded credentials.
 */
@Component
@Order(10)
public class DefaultGovernmentOfficerInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DefaultGovernmentOfficerInitializer.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public DefaultGovernmentOfficerInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    seedOfficer("officer@vetra.gov.in", "+919876543210", "Password@123", UserRole.GOVERNMENT_OFFICER);
    seedOfficer("admin@vetra.gov.in", "+919876543211", "Password@123", UserRole.ADMINISTRATOR);
  }

  private void seedOfficer(String email, String phone, String rawPassword, UserRole role) {
    User user = userRepository.findByEmail(email).orElse(null);
    if (user == null) {
      user = User.builder()
          .email(email)
          .phone(phone)
          .passwordHash(passwordEncoder.encode(rawPassword))
          .role(role)
          .isActive(true)
          .preferredLanguage("en")
          .build();
      userRepository.save(user);
      log.info("Seeded default {} account: {}", role, email);
    } else {
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
      user.setRole(role);
      user.setActive(true);
      userRepository.save(user);
      log.info("Updated default {} credentials: {}", role, email);
    }
  }
}
