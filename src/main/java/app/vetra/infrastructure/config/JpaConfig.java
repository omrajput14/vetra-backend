package app.vetra.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration.
 *
 * <p>Enables JPA auditing for automatic handling of {@code @CreatedDate} and
 * {@code @LastModifiedDate} entity fields.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {}
