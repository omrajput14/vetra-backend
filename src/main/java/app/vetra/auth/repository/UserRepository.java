package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access repository for User entity. */
public interface UserRepository extends JpaRepository<User, UUID> {

  /** Finds user by email. */
  Optional<User> findByEmail(String email);

  /** Finds user by phone. */
  Optional<User> findByPhone(String phone);

  /** Finds user by email or phone identifier. */
  @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.phone = :identifier")
  Optional<User> findByIdentifier(@Param("identifier") String identifier);

  /** Checks if email exists. */
  boolean existsByEmail(String email);

  /** Checks if phone exists. */
  boolean existsByPhone(String phone);
}
