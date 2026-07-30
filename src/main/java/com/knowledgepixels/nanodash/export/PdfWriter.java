package com.knowledgepixels.nanodash.export;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.EnumSet;

/**
 * Serialises a {@link DocumentModel} as a PDF document by rendering the
 * {@link HtmlWriter} XHTML output with openhtmltopdf. Text uses the PDF base-14
 * fonts; the bundled monochrome Noto Emoji font is registered as a final fallback
 * so the emoji in view titles render as glyphs instead of the {@code #}
 * missing-glyph replacement.
 */
public final class PdfWriter {

    private static final Logger logger = LoggerFactory.getLogger(PdfWriter.class);

    private static final String EMOJI_FONT_RESOURCE = "/fonts/NotoEmoji-Regular.ttf";

    private PdfWriter() {
    }

    /**
     * Serialises the model as a PDF document. If the embedded HTML of a
     * plain-paragraph view breaks the XHTML parser, the document is re-rendered
     * with those fragments replaced by their plain-text fallbacks.
     *
     * @param doc the document model
     * @return the PDF bytes
     */
    public static byte[] write(DocumentModel doc) {
        try {
            return render(HtmlWriter.writeXhtml(doc, false));
        } catch (Exception ex) {
            logger.warn("PDF rendering with embedded HTML failed, retrying with plain text: {}", ex.getMessage());
            try {
                return render(HtmlWriter.writeXhtml(doc, true));
            } catch (Exception ex2) {
                throw new RuntimeException("PDF rendering failed", ex2);
            }
        }
    }

    private static byte[] render(String xhtml) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useFont(() -> PdfWriter.class.getResourceAsStream(EMOJI_FONT_RESOURCE),
                "Noto Emoji", 400, BaseRendererBuilder.FontStyle.NORMAL, true,
                EnumSet.of(BaseRendererBuilder.FSFontUseCase.FALLBACK_FINAL));
        builder.withHtmlContent(xhtml, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

}
