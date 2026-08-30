package app.vetra.auth.repository;

import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  /** Counts users by role. */
  long countByRole(UserRole role);

  /** Counts active users. */
  long countByIsActiveTrue();

  /** Counts inactive users. */
  long countByIsActiveFalse();

  /** Counts users registered after a timestamp. */
  long countByCreatedAtAfter(Instant after);

  /** Finds most recently registered users. */
  List<User> findTop30ByOrderByCreatedAtDesc();

  /** Finds users by role with pagination. */
  Page<User> findByRole(UserRole role, Pageable pageable);

  /** Finds users by active status with pagination. */
  Page<User> findByIsActive(boolean isActive, Pageable pageable);

  /** Finds users by role and active status with pagination. */
  Page<User> findByRoleAndIsActive(UserRole role, boolean isActive, Pageable pageable);
}
