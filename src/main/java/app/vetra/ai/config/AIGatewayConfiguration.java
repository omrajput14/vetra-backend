package app.vetra.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration class for the AI Gateway layer. Enables binding of {@link AIGatewayProperties}
 * from the {@code vetra.ai.gateway} YAML namespace.
 */
@Configuration
@EnableConfigurationProperties({AIGatewayProperties.class, AgentProperties.class})
public class AIGatewayConfiguration {}
