package app.vetra.infrastructure.security;

import app.vetra.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Utility for issuing, parsing, and validating JWT access tokens. */
@Component
public class JwtUtil {

  private final JwtProperties jwtProperties;
  private final SecretKey secretKey;

  /** Constructor injection. */
  public JwtUtil(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Generates a signed JWT access token.
   *
   * @param email subject identifier
   * @param role user role
   * @return signed JWT string
   */
  public String generateAccessToken(String email, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProperties.expirationMs());

    return Jwts.builder()
        .subject(email)
        .claim("role", role)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Validates token signature and expiration against subject.
   *
   * @param token JWT string
   * @param userEmail expected subject email
   * @return true if valid
   */
  public boolean isTokenValid(String token, String userEmail) {
    String subject = extractSubject(token);
    return subject.equalsIgnoreCase(userEmail) && !isTokenExpired(token);
  }

  /** Extracts subject email from token. */
  public String extractSubject(String token) {
    return extractAllClaims(token).getSubject();
  }

  /** Extracts role claim from token. */
  public String extractRole(String token) {
    return extractAllClaims(token).get("role", String.class);
  }

  /** Extracts expiration date from token. */
  public Date extractExpiration(String token) {
    return extractAllClaims(token).getExpiration();
  }

  public long getExpirationMs() {
    return jwtProperties.expirationMs();
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
