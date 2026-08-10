package com.knowledgepixels.nanodash.export;

import com.knowledgepixels.nanodash.export.DocumentModel.Block;
import com.knowledgepixels.nanodash.export.DocumentModel.HtmlBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Inline;
import com.knowledgepixels.nanodash.export.DocumentModel.ListBlock;
import com.knowledgepixels.nanodash.export.DocumentModel.Paragraph;
import com.knowledgepixels.nanodash.export.DocumentModel.Section;
import com.knowledgepixels.nanodash.export.DocumentModel.TableBlock;

import java.util.List;

/**
 * Serialises a {@link DocumentModel} as a self-contained HTML document. All generated
 * markup is well-formed XHTML, so {@link #writeXhtml(DocumentModel, boolean)} can feed
 * the PDF renderer directly; the only possible source of ill-formedness is the embedded
 * sanitized fragment of an {@link HtmlBlock}, which can be replaced by its plain-text
 * fallback via the {@code stripEmbeddedHtml} flag.
 */
public final class HtmlWriter {

    private static final String STYLE = """
            body { font-family: Helvetica, Arial, sans-serif; font-size: 11pt; margin: 2em; color: #202020; }
            h1 { font-size: 18pt; font-weight: bold; margin: 0 0 0.2em; }
            h2 { font-size: 13pt; font-weight: bold; color: #333333; margin: 1.5em 0 0.5em; }
            p { margin: 0.4em 0; }
            p.doc-iri { color: #888888; font-size: 9pt; margin: 0 0 1em; }
            p.doc-description { color: #555555; }
            table { border-collapse: collapse; margin: 0.4em 0; }
            th, td { border: 1px solid #cccccc; padding: 4px 8px; text-align: left; vertical-align: top; }
            th { background-color: #f2f2f2; }
            a { color: #0B73DA; }
            @page { margin: 2cm; }
            """;

    private HtmlWriter() {
    }

    /**
     * Serialises the model as an HTML document for viewing in a browser.
     *
     * @param doc the document model
     * @return the HTML document
     */
    public static String write(DocumentModel doc) {
        return writeXhtml(doc, false);
    }

    /**
     * Serialises the model as a strict XHTML document (for the PDF renderer).
     *
     * @param doc               the document model
     * @param stripEmbeddedHtml whether to replace embedded HTML fragments by their
     *                          plain-text fallbacks (guaranteeing well-formedness)
     * @return the XHTML document
     */
    public static String writeXhtml(DocumentModel doc, boolean stripEmbeddedHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
        sb.append("<meta charset=\"utf-8\"/>\n");
        sb.append("<title>").append(escape(doc.title())).append("</title>\n");
        sb.append("<style>\n").append(STYLE).append("</style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("<h1>").append(escape(doc.title())).append("</h1>\n");
        if (doc.iri() != null) {
            sb.append("<p class=\"doc-iri\"><a href=\"").append(escape(doc.iri())).append("\">")
                    .append(escape(doc.iri())).append("</a></p>\n");
        }
        for (Section section : doc.sections()) {
            sb.append("<h2>").append(escape(section.heading())).append("</h2>\n");
            if (section.description() != null && !section.description().isBlank()) {
                sb.append("<p class=\"doc-description\">").append(escape(section.description())).append("</p>\n");
            }
            for (Block block : section.blocks()) {
                appendBlock(sb, block, stripEmbeddedHtml);
            }
        }
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private static void appendBlock(StringBuilder sb, Block block, boolean stripEmbeddedHtml) {
        if (block instanceof Paragraph paragraph) {
            sb.append("<p>");
            appendInlines(sb, paragraph.inlines());
            sb.append("</p>\n");
        } else if (block instanceof TableBlock table) {
            sb.append("<table>\n");
            if (table.headers() != null) {
                sb.append("<tr>");
                for (String header : table.headers()) {
                    sb.append("<th>").append(escape(header)).append("</th>");
                }
                sb.append("</tr>\n");
            }
            for (List<List<Inline>> row : table.rows()) {
                sb.append("<tr>");
                for (List<Inline> cell : row) {
                    sb.append("<td>");
                    appendInlines(sb, cell);
                    sb.append("</td>");
                }
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
        } else if (block instanceof ListBlock list) {
            sb.append("<ul>\n");
            for (List<Inline> item : list.items()) {
                sb.append("<li>");
                appendInlines(sb, item);
                sb.append("</li>\n");
            }
            sb.append("</ul>\n");
        } else if (block instanceof HtmlBlock html) {
            if (stripEmbeddedHtml) {
                sb.append("<p>").append(escape(html.plainTextFallback())).append("</p>\n");
            } else {
                sb.append("<div>").append(html.sanitizedHtml()).append("</div>\n");
            }
        }
    }

    private static void appendInlines(StringBuilder sb, List<Inline> inlines) {
        for (Inline inline : inlines) {
            if (inline.href() != null) {
                sb.append("<a href=\"").append(escape(inline.href())).append("\">")
                        .append(escape(inline.text())).append("</a>");
            } else {
                sb.append(escape(inline.text()));
            }
        }
    }

    /**
     * Escapes a string for use in XHTML text content and attribute values.
     *
     * @param s the string (may be null)
     * @return the escaped string (empty for null)
     */
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

}
