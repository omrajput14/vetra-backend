package app.vetra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Vetra Backend – Livestock and Veterinary Healthcare Platform.
 *
 * <p>Entry point for the Spring Boot application. Feature modules are loaded via component scan
 * from sub-packages under {@code app.vetra}.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class VetraApplication {

  /**
   * Main method – starts the embedded Tomcat server.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(VetraApplication.class, args);
  }
}
