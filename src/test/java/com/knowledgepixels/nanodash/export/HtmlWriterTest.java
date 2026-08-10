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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlWriterTest {

    private static DocumentModel model(DocumentModel.Block... blocks) {
        return new DocumentModel("My Title", "https://example.org/resource",
                List.of(new Section("Section One", "A description", List.of(blocks))));
    }

    @Test
    @DisplayName("should escape HTML special characters in text")
    void escapesText() {
        String html = HtmlWriter.write(model(Paragraph.of("<script>alert('x & y')</script>")));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x &amp; y&#39;)&lt;/script&gt;"));
    }

    @Test
    @DisplayName("should render title, IRI, heading and description")
    void rendersStructure() {
        String html = HtmlWriter.write(model(Paragraph.of("Hello")));
        assertTrue(html.contains("<h1>My Title</h1>"));
        assertTrue(html.contains("https://example.org/resource"));
        assertTrue(html.contains("<h2>Section One</h2>"));
        assertTrue(html.contains("A description"));
        assertTrue(html.contains("<p>Hello</p>"));
    }

    @Test
    @DisplayName("should render tables with headers and linked cells")
    void rendersTable(){
        TableBlock table = new TableBlock(List.of("Name", ""),
                List.of(List.of(
                        List.of(new Inline("Alice", "https://example.org/alice")),
                        List.of(new Inline("plain", null)))));
        String html = HtmlWriter.write(model(table));
        assertTrue(html.contains("<th>Name</th><th></th>"));
        assertTrue(html.contains("<a href=\"https://example.org/alice\">Alice</a>"));
        assertTrue(html.contains("<td>plain</td>"));
    }

    @Test
    @DisplayName("should render list items")
    void rendersList() {
        String html = HtmlWriter.write(model(new ListBlock(List.of(List.of(new Inline("item one", null))))));
        assertTrue(html.contains("<li>item one</li>"));
    }

    @Test
    @DisplayName("should self-close void elements for XHTML compatibility")
    void selfClosesVoids() {
        String html = HtmlWriter.write(model(Paragraph.of("x")));
        assertTrue(html.contains("<meta charset=\"utf-8\"/>"));
    }

    @Test
    @DisplayName("should pass embedded HTML through by default and strip it when requested")
    void embeddedHtmlPassthroughAndStrip() {
        HtmlBlock block = new HtmlBlock("<p>rich <b>content</b></p>", "rich content");
        String html = HtmlWriter.writeXhtml(model(block), false);
        assertTrue(html.contains("<p>rich <b>content</b></p>"));

        String stripped = HtmlWriter.writeXhtml(model(block), true);
        assertFalse(stripped.contains("<b>"));
        assertTrue(stripped.contains("<p>rich content</p>"));
    }

}
