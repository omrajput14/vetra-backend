package app.vetra.ai.rag.ingestion;

import app.vetra.ai.rag.model.KnowledgeDocument;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Dedicated document parser responsible for cleaning, sanitizing, and normalizing raw veterinary
 * clinical literature across plain text and markdown formats.
 */
@Component
public class DocumentParser {

  /**
   * Parses and cleans the raw content of a knowledge document, normalizing whitespace and
   * extracting contextual metadata.
   *
   * @param document raw knowledge document
   * @return normalized document text suitable for chunking
   */
  public String parse(KnowledgeDocument document) {
    if (document == null || document.rawContent() == null) {
      return "";
    }

    String content = document.rawContent();
    String format = document.format() != null ? document.format().toUpperCase(Locale.ROOT) : "TEXT";

    return switch (format) {
      case "MARKDOWN", "MD" -> parseMarkdown(content);
      default -> parsePlainText(content);
    };
  }

  /** Cleans markdown syntax while preserving structure. */
  private String parseMarkdown(String content) {
    return content
        .replaceAll("#+\\s*", "") // remove markdown headers
        .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // remove bold asterisks
        .replaceAll("\\*(.*?)\\*", "$1") // remove italic
        .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1") // strip links, keep text
        .replaceAll("\\r\\n|\\r", "\n")
        .replaceAll("\n{3,}", "\n\n")
        .trim();
  }

  /** Cleans plain text whitespace and control characters. */
  private String parsePlainText(String content) {
    return content
        .replaceAll("\\r\\n|\\r", "\n")
        .replaceAll("[\\t\\x0B\\f]+", " ")
        .replaceAll(" {2,}", " ")
        .replaceAll("\n{3,}", "\n\n")
        .trim();
  }

  /** Extracts inherited metadata and combines it with document properties. */
  public Map<String, String> extractMetadata(KnowledgeDocument document) {
    Map<String, String> meta = new HashMap<>();
    if (document.metadata() != null) {
      meta.putAll(document.metadata());
    }
    if (document.title() != null) {
      meta.put("title", document.title());
    }
    if (document.author() != null) {
      meta.put("author", document.author());
    }
    if (document.category() != null) {
      meta.put("category", document.category());
    }
    if (document.format() != null) {
      meta.put("format", document.format());
    }
    return meta;
  }
}
