package app.vetra.auth.service;

import app.vetra.auth.repository.UserRepository;
import app.vetra.infrastructure.persistence.entity.User;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Custom Spring Security UserDetailsService loading User from database. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /** Constructor injection. */
  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByIdentifier(identifier)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException("User not found with identifier: " + identifier));

    return new org.springframework.security.core.userdetails.User(
        user.getEmail() != null ? user.getEmail() : user.getPhone(),
        user.getPasswordHash(),
        user.isActive(),
        true,
        true,
        true,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
  }
}
