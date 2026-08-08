package app.vetra.ai.rag.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.vetra.ai.rag.model.KnowledgeDocument;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentParserTest {

  private DocumentParser parser;

  @BeforeEach
  void setUp() {
    parser = new DocumentParser();
  }

  @Test
  void testParseMarkdown() {
    String mdContent = "# Bovine Mastitis\n\n**Etiology**: *Staphylococcus aureus* is a major pathogen.\n\n[Reference](https://fao.org)";
    KnowledgeDocument doc =
        KnowledgeDocument.of(
            "doc-md",
            "Markdown Protocol",
            "Dr. Vet",
            "EPIDEMIOLOGY",
            mdContent,
            "MARKDOWN",
            Map.of("species", "CATTLE"));

    String parsed = parser.parse(doc);
    assertFalse(parsed.contains("#"));
    assertFalse(parsed.contains("**"));
    assertTrue(parsed.contains("Staphylococcus aureus"));
    assertTrue(parsed.contains("Reference"));

    Map<String, String> meta = parser.extractMetadata(doc);
    assertEquals("Markdown Protocol", meta.get("title"));
    assertEquals("Dr. Vet", meta.get("author"));
    assertEquals("CATTLE", meta.get("species"));
  }

  @Test
  void testParsePlainText() {
    String plain = "Line 1   with   spaces\n\n\nLine 2";
    KnowledgeDocument doc =
        KnowledgeDocument.of(
            "doc-plain",
            "Plain Doc",
            "Vet",
            "GENERAL",
            plain,
            "TEXT",
            Map.of());

    String parsed = parser.parse(doc);
    assertTrue(parsed.contains("Line 1 with spaces"));
    assertTrue(parsed.contains("Line 2"));
  }

  @Test
  void testParseNullDocument() {
    assertEquals("", parser.parse(null));
  }
}
