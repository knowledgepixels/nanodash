package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.DownloadRdfPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A reusable panel that renders download links for RDF formats (TriG, TriX, JSON-LD, N-Quads).
 */
public class DownloadRdfLinks extends Panel {

    /**
     * Creates download links for a top-level page (user, space, or resource).
     *
     * @param markupId the Wicket markup ID
     * @param type     "user", "space", or "resource"
     * @param id       the resource identifier
     */
    public DownloadRdfLinks(String markupId, String type, String id) {
        this(markupId, type, id, null);
    }

    /**
     * Creates download links for a part page.
     *
     * @param markupId  the Wicket markup ID
     * @param type      "part"
     * @param id        the part identifier
     * @param contextId the context resource ID (required for type=part)
     */
    public DownloadRdfLinks(String markupId, String type, String id, String contextId) {
        super(markupId);

        add(createLink("trig", type, id, contextId, "trig"));
        add(createLink("jsonld", type, id, contextId, "jsonld"));
        add(createLink("nq", type, id, contextId, "nq"));
        add(createLink("trix", type, id, contextId, "trix"));
    }

    private BookmarkablePageLink<Void> createLink(String wicketId, String type, String id, String contextId, String format) {
        PageParameters params = new PageParameters()
                .set("type", type)
                .set("id", id)
                .set("format", format);
        if (contextId != null) {
            params.set("context", contextId);
        }
        return new BookmarkablePageLink<>(wicketId, DownloadRdfPage.class, params);
    }

}
