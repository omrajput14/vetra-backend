package app.vetra.ai.rag.ingestion;

import app.vetra.ai.observability.AIMetricsCollector;
import app.vetra.ai.rag.embedding.EmbeddingProvider;
import app.vetra.ai.rag.model.KnowledgeChunk;
import app.vetra.ai.rag.model.KnowledgeDocument;
import app.vetra.ai.rag.store.VectorStore;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * End-to-end ingestion orchestrator for veterinary clinical documents.
 * Coordinates parsing, chunking, embedding generation, and vector indexing.
 */
@Service
public class DocumentIngestionService {

  private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

  private final DocumentParser documentParser;
  private final DocumentChunker documentChunker;
  private final EmbeddingProvider embeddingProvider;
  private final VectorStore vectorStore;
  private final AIMetricsCollector metricsCollector;

  /**
   * Constructs DocumentIngestionService.
   *
   * @param documentParser parser for raw text extraction
   * @param documentChunker chunker for sentence-aware segmentation
   * @param embeddingProvider embedding provider
   * @param vectorStore vector store index
   * @param metricsCollector operational metrics collector (optional)
   */
  public DocumentIngestionService(
      DocumentParser documentParser,
      DocumentChunker documentChunker,
      EmbeddingProvider embeddingProvider,
      VectorStore vectorStore,
      @Autowired(required = false) AIMetricsCollector metricsCollector) {
    this.documentParser = documentParser;
    this.documentChunker = documentChunker;
    this.embeddingProvider = embeddingProvider;
    this.vectorStore = vectorStore;
    this.metricsCollector = metricsCollector;
  }

  /**
   * Ingests a single knowledge document into the vector store.
   *
   * @param document domain document
   * @return list of indexed chunks
   */
  public List<KnowledgeChunk> ingestDocument(KnowledgeDocument document) {
    if (document == null) {
      return List.of();
    }

    log.info("Ingesting document id='{}', title='{}', category='{}'",
        document.id(), document.title(), document.category());

    String parsedText = documentParser.parse(document);
    Map<String, String> metadata = documentParser.extractMetadata(document);

    List<KnowledgeChunk> chunks = documentChunker.chunk(document, parsedText, metadata);
    if (chunks.isEmpty()) {
      log.warn("Document id='{}' produced 0 chunks after parsing and chunking", document.id());
      return List.of();
    }

    List<String> chunkTexts = chunks.stream().map(KnowledgeChunk::content).toList();
    List<float[]> vectors = embeddingProvider.generateEmbeddings(chunkTexts);

    vectorStore.upsert(chunks, vectors);

    if (metricsCollector != null) {
      metricsCollector.recordRagIngestion(1, chunks.size());
    }

    log.info("Successfully ingested document id='{}': generated {} chunks", document.id(), chunks.size());
    return chunks;
  }

  /**
   * Ingests multiple documents in batch.
   *
   * @param documents list of documents
   * @return total count of indexed chunks
   */
  public int ingestBatch(List<KnowledgeDocument> documents) {
    if (documents == null || documents.isEmpty()) {
      return 0;
    }
    int totalChunks = 0;
    for (KnowledgeDocument doc : documents) {
      totalChunks += ingestDocument(doc).size();
    }
    return totalChunks;
  }

  /**
   * Removes a document and all its chunks from the vector store.
   *
   * @param documentId document identifier
   */
  public void deleteDocument(String documentId) {
    if (documentId != null) {
      vectorStore.delete(documentId);
      log.info("Deleted document id='{}' from vector store", documentId);
    }
  }
}
