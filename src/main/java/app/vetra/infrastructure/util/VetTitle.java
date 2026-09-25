package app.vetra.infrastructure.util;

/** Formats a veterinarian's name with the "Dr." title for user-facing messages. */
public final class VetTitle {

  private VetTitle() {
    // Utility class private constructor
  }

  /**
   * Returns the name with a "Dr." prefix, unless it already starts with one (many vets register
   * as "Dr. Ananya Roy", which previously produced "Dr. Dr. Ananya Roy").
   *
   * @param name the vet's full name as stored
   * @return the name as it should appear in messages
   */
  public static String of(String name) {
    if (name == null || name.isBlank()) {
      return "Dr.";
    }
    String trimmed = name.trim();
    if (trimmed.matches("(?i)^dr\\b.*")) {
      return trimmed;
    }
    return "Dr. " + trimmed;
  }
}
