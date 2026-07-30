package app.vetra.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Custom AuthenticationEntryPoint serializing standardized Error Catalogue envelopes for unauthenticated / expired JWT requests.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    String authHeader = request.getHeader("Authorization");
    String errorCode = "AUTH_003";
    String errorMessage = "Authentication token is invalid or missing.";

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      errorCode = "AUTH_002";
      errorMessage = "Your session has expired. Please log in again.";
    }

    Map<String, Object> errorObj = new LinkedHashMap<>();
    errorObj.put("code", errorCode);
    errorObj.put("message", errorMessage);
    errorObj.put("details", null);
    errorObj.put("timestamp", Instant.now().toString());
    errorObj.put("path", request.getRequestURI());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("status", HttpStatus.UNAUTHORIZED.value());
    body.put("message", errorMessage);
    body.put("data", null);
    body.put("error", errorObj);
    body.put("timestamp", Instant.now().toString());

    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
