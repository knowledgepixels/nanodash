package com.knowledgepixels.nanodash.export;

import com.knowledgepixels.nanodash.export.DocumentModel.HtmlBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Inline;
import com.knowledgepixels.nanodash.export.DocumentModel.ListBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Paragraph;
import com.knowledgepixels.nanodash.export.DocumentModel.Section;
import com.knowledgepixels.nanodash.export.DocumentModel.TableBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtfWriterTest {

    private static DocumentModel model(DocumentModel.Block... blocks) {
        return new DocumentModel("My Title", "https://example.org/resource",
                List.of(new Section("Section One", null, List.of(blocks))));
    }

    private static long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }

    @Test
    @DisplayName("should produce an RTF document with balanced braces")
    void prologAndBalancedBraces() {
        String rtf = RtfWriter.write(model(Paragraph.of("Hello")));
        assertTrue(rtf.startsWith("{\\rtf1\\ansi"));
        assertTrue(rtf.trim().endsWith("}"));
        assertEquals(count(rtf, '{'), count(rtf, '}'), "braces should be balanced");
    }

    @Test
    @DisplayName("should escape RTF control characters")
    void escapesControlCharacters() {
        assertEquals("a\\{b\\}c\\\\d", RtfWriter.escape("a{b}c\\d"));
    }

    @Test
    @DisplayName("should encode non-ASCII characters as unicode escapes")
    void encodesNonAscii() {
        assertEquals("caf\\u233?", RtfWriter.escape("café"));
        // Astral characters become surrogate pairs (Word convention) with signed 16-bit values
        String emoji = RtfWriter.escape("🚀");
        assertEquals("\\u-10179?\\u-8576?", emoji);
    }

    @Test
    @DisplayName("should render hyperlinks as HYPERLINK fields")
    void rendersHyperlinks() {
        String rtf = RtfWriter.write(model(new Paragraph(List.of(new Inline("Alice", "https://example.org/alice")))));
        assertTrue(rtf.contains("{\\field{\\*\\fldinst{HYPERLINK \"https://example.org/alice\"}}{\\fldrslt{\\ul\\cf2 Alice}}}"));
    }

    @Test
    @DisplayName("should render tables with row and cell markers")
    void rendersTable() {
        TableBlock table = new TableBlock(List.of("Name", "Value"),
                List.of(List.of(
                        List.of(new Inline("a", null)),
                        List.of(new Inline("b", null)))));
        String rtf = RtfWriter.write(model(table));
        assertTrue(rtf.contains("\\trowd"));
        assertTrue(rtf.contains("\\cellx4680"));
        assertTrue(rtf.contains("\\cellx9360"));
        assertTrue(rtf.contains("{\\b Name}\\cell"));
        assertTrue(rtf.contains("\\row"));
    }

    @Test
    @DisplayName("should render list items as bullets")
    void rendersList() {
        String rtf = RtfWriter.write(model(new ListBlock(List.of(List.of(new Inline("item", null))))));
        assertTrue(rtf.contains("\\bullet\\tab item\\par"));
    }

    @Test
    @DisplayName("should use the plain-text fallback for HTML blocks")
    void htmlBlockFallback() {
        String rtf = RtfWriter.write(model(new HtmlBlock("<p>rich <b>content</b></p>", "rich content")));
        assertTrue(rtf.contains("rich content"));
        assertFalse(rtf.contains("<p>"));
    }

}
