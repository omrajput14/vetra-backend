package app.vetra.notification.provider;

/**
 * Result payload returned by notification provider dispatch execution.
 *
 * @param success true if provider successfully accepted notification
 * @param providerName identifier of notification provider
 * @param messageId provider transaction or message ID
 * @param errorMessage error message if transmission failed
 */
public record NotificationProviderResult(
    boolean success,
    String providerName,
    String messageId,
    String errorMessage
) {

  /**
   * Factory method for successful transmission result.
   *
   * @param providerName provider identifier
   * @param messageId message ID returned by gateway
   * @return {@link NotificationProviderResult}
   */
  public static NotificationProviderResult ok(String providerName, String messageId) {
    return new NotificationProviderResult(true, providerName, messageId, null);
  }

  /**
   * Factory method for failed transmission result.
   *
   * @param providerName provider identifier
   * @param errorMessage failure detail description
   * @return {@link NotificationProviderResult}
   */
  public static NotificationProviderResult error(String providerName, String errorMessage) {
    return new NotificationProviderResult(false, providerName, null, errorMessage);
  }
}
