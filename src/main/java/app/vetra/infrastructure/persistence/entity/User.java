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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public static UserBuilder builder() {
    return new UserBuilder();
  }

  public static class UserBuilder {
    private String email;
    private String phone;
    private String passwordHash;
    private UserRole role;
    private boolean isActive = true;

    public UserBuilder email(String email) {
      this.email = email;
      return this;
    }

    public UserBuilder phone(String phone) {
      this.phone = phone;
      return this;
    }

    public UserBuilder passwordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public UserBuilder role(UserRole role) {
      this.role = role;
      return this;
    }

    public UserBuilder isActive(boolean isActive) {
      this.isActive = isActive;
      return this;
    }

    public User build() {
      User user = new User();
      user.setEmail(this.email);
      user.setPhone(this.phone);
      user.setPasswordHash(this.passwordHash);
      user.setRole(this.role);
      user.setActive(this.isActive);
      return user;
    }
  }
}
