package app.vetra.ai.rag.ingestion;

import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.KnowledgeDocument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Enterprise text chunker implementing a sliding-window segmentation strategy that preserves
 * sentence boundaries and contextual overlap across clinical paragraphs.
 */
@Component
public class DocumentChunker {

  public static final int DEFAULT_CHUNK_SIZE = 500;
  public static final int DEFAULT_OVERLAP = 50;

  /**
   * Chunks a parsed document into a list of segmented {@link KnowledgeChunk} records using default
   * parameters.
   *
   * @param document parent knowledge document
   * @param parsedText cleaned document content
   * @param metadata combined metadata map
   * @return list of segmented knowledge chunks
   */
  public List<KnowledgeChunk> chunk(
      KnowledgeDocument document, String parsedText, Map<String, String> metadata) {
    return chunk(document, parsedText, metadata, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
  }

  /**
   * Chunks a parsed document into a list of segmented {@link KnowledgeChunk} records with custom
   * size and overlap.
   *
   * @param document parent knowledge document
   * @param parsedText cleaned document content
   * @param metadata combined metadata map
   * @param chunkSize target chunk character length
   * @param overlap character overlap between consecutive chunks
   * @return list of segmented knowledge chunks
   */
  public List<KnowledgeChunk> chunk(
      KnowledgeDocument document,
      String parsedText,
      Map<String, String> metadata,
      int chunkSize,
      int overlap) {

    if (parsedText == null || parsedText.isBlank()) {
      return Collections.emptyList();
    }

    int effectiveChunkSize = Math.max(100, chunkSize);
    int effectiveOverlap = Math.max(0, Math.min(overlap, effectiveChunkSize / 2));

    List<KnowledgeChunk> chunks = new ArrayList<>();
    int length = parsedText.length();
    int start = 0;
    int chunkIndex = 0;

    while (start < length) {
      int end = Math.min(start + effectiveChunkSize, length);

      // If we aren't at the end of the text, try to break on a sentence or paragraph boundary
      if (end < length) {
        int sentenceBreak = findSentenceBreak(parsedText, start, end);
        if (sentenceBreak > start + (effectiveChunkSize / 2)) {
          end = sentenceBreak;
        }
      }

      String chunkContent = parsedText.substring(start, end).trim();
      if (!chunkContent.isEmpty()) {
        int tokenCount = Math.max(1, chunkContent.length() / 4);
        String chunkId = document.id() + "-chunk-" + chunkIndex;

        chunks.add(
            new KnowledgeChunk(
                chunkId,
                document.id(),
                document.title(),
                chunkIndex,
                chunkContent,
                tokenCount,
                metadata != null ? Map.copyOf(metadata) : Map.of()));
        chunkIndex++;
      }

      // Step forward by (end - overlap)
      if (end >= length) {
        break;
      }
      start = Math.max(start + 1, end - effectiveOverlap);
    }

    return chunks;
  }

  /** Locates the nearest sentence or paragraph boundary before the chunk end. */
  private int findSentenceBreak(String text, int start, int end) {
    // Search backward from end for paragraph break
    int newline = text.lastIndexOf("\n\n", end);
    if (newline > start) {
      return newline + 2;
    }

    // Search for sentence ending punctuation (.!?) followed by whitespace
    for (int i = end - 1; i > start; i--) {
      char c = text.charAt(i);
      if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
        return i + 1;
      }
    }

    // Fall back to space
    int space = text.lastIndexOf(' ', end);
    if (space > start) {
      return space + 1;
    }

    return end;
  }
}
