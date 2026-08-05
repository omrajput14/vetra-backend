package app.vetra.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AWS S3 configuration properties.
 *
 * <p>Bound from {@code vetra.aws.*} in application.yml. The actual S3 client bean is created in
 * {@link S3Config}.
 */
@Validated
@ConfigurationProperties(prefix = "vetra.aws")
public record AwsProperties(@NotBlank String region, Credentials credentials, S3Properties s3) {

  /** AWS IAM credential pair. */
  public record Credentials(String accessKey, String secretKey) {}

  /** S3 bucket configuration. */
  public record S3Properties(
      @NotBlank String bucketName, @Positive int presignedUrlExpiryMinutes) {}
}
