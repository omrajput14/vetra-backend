package app.vetra.infrastructure.persistence.entity;

import app.vetra.infrastructure.persistence.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Core system user entity. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  @Column(name = "email", unique = true)
  private String email;

  @Column(name = "phone", unique = true)
  private String phone;

  @NotNull
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 30)
  private UserRole role;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;
}
