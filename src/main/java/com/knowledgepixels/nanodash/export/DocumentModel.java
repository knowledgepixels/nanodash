package com.knowledgepixels.nanodash.export;

import java.io.Serializable;
import java.util.List;

/**
 * Format-neutral representation of a resource page's content: the resource title
 * and IRI, followed by one section per view display. The writers ({@link HtmlWriter},
 * {@link RtfWriter}, {@link PdfWriter}) serialise this model into concrete formats.
 *
 * @param title    the resource label
 * @param iri      the resource IRI, shown under the title
 * @param sections the document sections, one per view display
 */
public record DocumentModel(String title, String iri, List<Section> sections) implements Serializable {

    /**
     * One document section, corresponding to one view display.
     *
     * @param heading     the section heading (the view display title)
     * @param description an optional description shown under the heading, or null
     * @param blocks      the section's content blocks
     */
    public record Section(String heading, String description, List<Block> blocks) implements Serializable {
    }

    /**
     * A content block within a section.
     */
    public sealed interface Block extends Serializable permits Paragraph, TableBlock, ListBlock, HtmlBlock {
    }

    /**
     * A plain paragraph of inline elements.
     *
     * @param inlines the paragraph content
     */
    public record Paragraph(List<Inline> inlines) implements Block {

        /**
         * Creates a paragraph with a single plain-text inline.
         *
         * @param text the paragraph text
         * @return the paragraph
         */
        public static Paragraph of(String text) {
            return new Paragraph(List.of(new Inline(text, null)));
        }
    }

    /**
     * A table of rows of cells, each cell a list of inlines.
     *
     * @param headers the column headers (empty strings for headerless columns), or null for no header row
     * @param rows    the table rows, each a list of cells, each cell a list of inlines
     */
    public record TableBlock(List<String> headers, List<List<List<Inline>>> rows) implements Block {
    }

    /**
     * A bullet list, each item a list of inlines.
     *
     * @param items the list items
     */
    public record ListBlock(List<List<Inline>> items) implements Block {
    }

    /**
     * A block of sanitized HTML (from a plain-paragraph view), with a pre-computed
     * plain-text fallback for formats that cannot embed HTML.
     *
     * @param sanitizedHtml     the sanitized HTML fragment
     * @param plainTextFallback the fragment stripped down to plain text
     */
    public record HtmlBlock(String sanitizedHtml, String plainTextFallback) implements Block {
    }

    /**
     * An inline element: a piece of text, optionally hyperlinked.
     *
     * @param text the text
     * @param href the link target, or null for plain text
     */
    public record Inline(String text, String href) implements Serializable {
    }

}
