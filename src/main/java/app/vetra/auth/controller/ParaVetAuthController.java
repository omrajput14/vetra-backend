package app.vetra.auth.controller;

import app.vetra.auth.dto.AuthResponse;
import app.vetra.auth.dto.LoginRequest;
import app.vetra.auth.repository.ParaVetProfileRepository;
import app.vetra.auth.repository.UserRepository;
import app.vetra.auth.service.AuthService;
import app.vetra.infrastructure.exception.ConflictException;
import app.vetra.infrastructure.persistence.entity.ParaVetProfile;
import app.vetra.infrastructure.persistence.entity.User;
import app.vetra.infrastructure.persistence.enums.UserRole;
import app.vetra.infrastructure.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Para-vet sign-up: creates the account and profile, then logs in. */
// ponytail: no admin approval step for para-vets yet; add one before a public launch.
@RestController
@RequestMapping("/api/v1/auth/paravet")
public class ParaVetAuthController {

  /** Para-vet registration payload. */
  public record ParaVetRegisterRequest(
      @NotBlank @Email String email,
      String phone,
      @NotBlank @Size(min = 8) String password,
      @NotBlank String fullName,
      String district,
      String taluka,
      Double latitude,
      Double longitude) {}

  private final UserRepository userRepository;
  private final ParaVetProfileRepository paraVetProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthService authService;

  public ParaVetAuthController(
      UserRepository userRepository,
      ParaVetProfileRepository paraVetProfileRepository,
      PasswordEncoder passwordEncoder,
      AuthService authService) {
    this.userRepository = userRepository;
    this.paraVetProfileRepository = paraVetProfileRepository;
    this.passwordEncoder = passwordEncoder;
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public ApiResponse<AuthResponse> register(@Valid @RequestBody ParaVetRegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("Email is already registered", "USER_001");
    }
    String phone = request.phone() != null && !request.phone().isBlank() ? request.phone() : null;
    if (phone != null && userRepository.existsByPhone(phone)) {
      throw new ConflictException("Phone number is already registered", "USER_002");
    }
    User user =
        userRepository.save(
            User.builder()
                .email(request.email())
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.PARA_VET)
                .isActive(true)
                .preferredLanguage("en")
                .build());
    ParaVetProfile profile = new ParaVetProfile();
    profile.setUser(user);
    profile.setFullName(request.fullName().trim());
    profile.setDistrict(request.district());
    profile.setTaluka(request.taluka());
    profile.setLatitude(request.latitude());
    profile.setLongitude(request.longitude());
    paraVetProfileRepository.save(profile);
    return ApiResponse.created(
        "Para-vet registered successfully",
        authService.loginUser(new LoginRequest(request.email(), request.password())));
  }
}
