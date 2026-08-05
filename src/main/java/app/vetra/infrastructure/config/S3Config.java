package app.vetra.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS SDK v2 configuration.
 *
 * <p>Provides an {@link S3Client} and {@link S3Presigner} bean. When {@code
 * vetra.aws.credentials.access-key} is blank (local dev or IAM role on EC2/ECS), the SDK falls back
 * to the {@link DefaultCredentialsProvider} chain automatically.
 */
@Configuration
public class S3Config {

  private final AwsProperties awsProperties;

  /** Constructor injection. */
  public S3Config(AwsProperties awsProperties) {
    this.awsProperties = awsProperties;
  }

  /**
   * Resolves AWS credentials. Uses static keys when explicitly configured; falls back to the
   * default provider chain (IAM role, environment variables, instance profile) otherwise.
   *
   * @return resolved credentials provider
   */
  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    String accessKey = awsProperties.credentials().accessKey();
    String secretKey = awsProperties.credentials().secretKey();

    if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
      return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
    return DefaultCredentialsProvider.create();
  }

  /**
   * AWS S3 synchronous client.
   *
   * @return configured {@link S3Client}
   */
  @Bean
  public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
    return S3Client.builder()
        .region(Region.of(awsProperties.region()))
        .credentialsProvider(credentialsProvider)
        .build();
  }

  /**
   * AWS S3 presigner for generating pre-signed upload / download URLs.
   *
   * @return configured {@link S3Presigner}
   */
  @Bean
  public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {
    return S3Presigner.builder()
        .region(Region.of(awsProperties.region()))
        .credentialsProvider(credentialsProvider)
        .build();
  }
}
