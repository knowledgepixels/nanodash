package com.knowledgepixels.nanodash.export;

import com.knowledgepixels.nanodash.export.DocumentModel.HtmlBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Paragraph;
import com.knowledgepixels.nanodash.export.DocumentModel.Section;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfWriterTest {

    @Test
    @DisplayName("should render a small model to a PDF")
    void rendersPdf() {
        DocumentModel doc = new DocumentModel("Test Title", "https://example.org/x",
                List.of(new Section("Section", "desc", List.of(Paragraph.of("Hello PDF")))));

        byte[] pdf = PdfWriter.write(doc);

        assertTrue(pdf.length > 100);
        String magic = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertTrue(magic.startsWith("%PDF-"), "should start with PDF magic bytes but got: " + magic);
    }

    @Test
    @DisplayName("should render emoji as glyphs via the fallback font, not as '#'")
    void rendersEmojiWithoutReplacementCharacter() throws Exception {
        DocumentModel doc = new DocumentModel("Test", null,
                List.of(new Section("📦 Maintained resources", null, List.of(Paragraph.of("News 📢 here")))));

        byte[] pdf = PdfWriter.write(doc);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertFalse(text.contains("#"), "missing-glyph '#' replacement in: " + text);
            assertTrue(text.contains("📦 Maintained resources"), "emoji heading missing in: " + text);
        }
    }

    @Test
    @DisplayName("should fall back to plain text when embedded HTML is ill-formed")
    void fallsBackOnBrokenHtml() {
        // Unclosed tag breaks the strict XHTML parse; the writer must retry with the fallback
        HtmlBlock broken = new HtmlBlock("<p>unclosed <b>tag", "unclosed tag");
        DocumentModel doc = new DocumentModel("Test", null,
                List.of(new Section("S", null, List.of(broken))));

        byte[] pdf = PdfWriter.write(doc);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
    }

}
