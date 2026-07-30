package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.export.DocumentModel;
import com.knowledgepixels.nanodash.export.DocumentModelBuilder;
import com.knowledgepixels.nanodash.export.HtmlWriter;
import com.knowledgepixels.nanodash.export.PdfWriter;
import com.knowledgepixels.nanodash.export.RtfWriter;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Page that serves the content of a resource page as a downloadable document.
 * The document contains the resource title and IRI followed by the rendered view
 * displays, as on the page's Content tab.
 *
 * Parameters:
 * - type: "user", "space", "resource", or "part"
 * - id: the resource identifier
 * - context: (required for type=part) the context resource ID
 * - format: "pdf" (default), "rtf", or "html"
 */
public class DownloadDocPage extends WebPage {

    private static final Logger logger = LoggerFactory.getLogger(DownloadDocPage.class);

    public static final String MOUNT_PATH = "/download-doc";

    static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "html", "text/html; charset=utf-8",
            "rtf", "application/rtf",
            "pdf", "application/pdf"
    );

    static final Map<String, String> EXTENSION_MAP = Map.of(
            "html", ".html",
            "rtf", ".rtf",
            "pdf", ".pdf"
    );

    public DownloadDocPage(final PageParameters parameters) {
        super(parameters);

        String type = parameters.get("type").toString();
        String id = parameters.get("id").toString();
        String format = parameters.get("format").toString("pdf");

        if (type == null) {
            throw new IllegalArgumentException("Parameter 'type' is required");
        }
        if (id == null) {
            throw new IllegalArgumentException("Parameter 'id' is required");
        }
        String contentType = CONTENT_TYPE_MAP.get(format);
        if (contentType == null) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        DocumentModel doc = DocumentModelBuilder.build(type, id, parameters);

        byte[] bytes = switch (format) {
            case "html" -> HtmlWriter.write(doc).getBytes(StandardCharsets.UTF_8);
            case "rtf" -> RtfWriter.write(doc).getBytes(StandardCharsets.US_ASCII);
            default -> PdfWriter.write(doc);
        };

        logger.info("Serving document download: {} format ({} bytes) for {} {}", format, bytes.length, type, id);

        String safeId = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeId.length() > 60) safeId = safeId.substring(safeId.length() - 60);
        String filename = type + "_" + safeId + EXTENSION_MAP.get(format);

        AbstractResourceStreamWriter stream = new AbstractResourceStreamWriter() {
            @Override
            public void write(OutputStream output) throws IOException {
                output.write(bytes);
            }

            @Override
            public String getContentType() {
                return contentType;
            }
        };

        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, filename);
        // HTML is the "view it in the browser" format; RTF and PDF are files by nature.
        handler.setContentDisposition("html".equals(format) ? ContentDisposition.INLINE : ContentDisposition.ATTACHMENT);
        handler.setCacheDuration(java.time.Duration.ZERO);
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

}
