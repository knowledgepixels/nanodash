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
 * Serialises a {@link DocumentModel} as an RTF document, which opens natively in
 * Word and LibreOffice. Hand-rolled: headings, paragraphs, hyperlink fields,
 * simple tables, and bullet lists via hanging indents.
 */
public final class RtfWriter {

    /**
     * Total table width in twips (6.5 inches), split equally over the columns.
     */
    private static final int TABLE_WIDTH_TWIPS = 9360;

    private RtfWriter() {
    }

    /**
     * Serialises the model as an RTF document.
     *
     * @param doc the document model
     * @return the RTF document
     */
    public static String write(DocumentModel doc) {
        StringBuilder sb = new StringBuilder();
        // Color 1: gray (IRI line), color 2: link blue
        sb.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\n");
        sb.append("{\\fonttbl{\\f0\\fswiss Helvetica;}}\n");
        sb.append("{\\colortbl ;\\red136\\green136\\blue136;\\red11\\green115\\blue218;}\n");
        sb.append("\\f0\\fs22\n");

        sb.append("\\pard\\sa60\\b\\fs36 ").append(escape(doc.title())).append("\\b0\\fs22\\par\n");
        if (doc.iri() != null) {
            sb.append("\\pard\\sa180\\cf1\\fs18 ").append(escape(doc.iri())).append("\\cf0\\fs22\\par\n");
        }

        for (Section section : doc.sections()) {
            sb.append("\\pard\\sb240\\sa60\\b\\fs26 ").append(escape(section.heading())).append("\\b0\\fs22\\par\n");
            if (section.description() != null && !section.description().isBlank()) {
                sb.append("\\pard\\sa60\\cf1 ").append(escape(section.description())).append("\\cf0\\par\n");
            }
            for (Block block : section.blocks()) {
                appendBlock(sb, block);
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static void appendBlock(StringBuilder sb, Block block) {
        if (block instanceof Paragraph paragraph) {
            sb.append("\\pard\\sa60 ");
            appendInlines(sb, paragraph.inlines());
            sb.append("\\par\n");
        } else if (block instanceof TableBlock table) {
            int columnCount = table.headers() != null ? table.headers().size()
                    : table.rows().isEmpty() ? 1 : table.rows().get(0).size();
            if (table.headers() != null) {
                appendRowStart(sb, columnCount);
                for (String header : table.headers()) {
                    sb.append("\\intbl {\\b ").append(escape(header)).append("}\\cell\n");
                }
                sb.append("\\row\n");
            }
            for (List<List<Inline>> row : table.rows()) {
                appendRowStart(sb, columnCount);
                for (List<Inline> cell : row) {
                    sb.append("\\intbl ");
                    appendInlines(sb, cell);
                    sb.append("\\cell\n");
                }
                sb.append("\\row\n");
            }
            sb.append("\\pard\\sa60\\par\n");
        } else if (block instanceof ListBlock list) {
            for (List<Inline> item : list.items()) {
                sb.append("\\pard\\fi-360\\li360 \\bullet\\tab ");
                appendInlines(sb, item);
                sb.append("\\par\n");
            }
            sb.append("\\pard\\sa60\n");
        } else if (block instanceof HtmlBlock html) {
            sb.append("\\pard\\sa60 ").append(escape(html.plainTextFallback())).append("\\par\n");
        }
    }

    private static void appendRowStart(StringBuilder sb, int columnCount) {
        sb.append("\\trowd\\trgaph108");
        for (int i = 1; i <= columnCount; i++) {
            sb.append("\\cellx").append(TABLE_WIDTH_TWIPS * i / columnCount);
        }
        sb.append("\n");
    }

    private static void appendInlines(StringBuilder sb, List<Inline> inlines) {
        for (Inline inline : inlines) {
            if (inline.href() != null) {
                sb.append("{\\field{\\*\\fldinst{HYPERLINK \"").append(escapeUrl(inline.href()))
                        .append("\"}}{\\fldrslt{\\ul\\cf2 ").append(escape(inline.text())).append("}}}");
            } else {
                sb.append(escape(inline.text()));
            }
        }
    }

    /**
     * Escapes a string for RTF: backslash-escapes the control characters, turns
     * newlines into line breaks, and encodes non-ASCII characters as backslash-u
     * escapes (one per UTF-16 code unit, so astral characters like emoji become
     * surrogate pairs, per Word convention).
     *
     * @param s the string (may be null)
     * @return the escaped string (empty for null)
     */
    static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '{' || c == '}') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\line ");
            } else if (c == '\r') {
                // skip
            } else if (c < 128) {
                sb.append(c);
            } else {
                // the backslash-u control word takes a signed 16-bit value; '?' is the fallback for old readers
                sb.append("\\u").append((int) (short) c).append('?');
            }
        }
        return sb.toString();
    }

    private static String escapeUrl(String url) {
        // Quotes would terminate the HYPERLINK field argument
        return escape(url.replace("\"", "%22"));
    }

}
