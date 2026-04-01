package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.page.DownloadRdfPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A reusable panel that renders "Raw page content" download links for RDF formats,
 * used on user/space/resource pages to download all nanopubs on the page.
 * Each format has a native-type link and a text/plain link (txt) that always displays in the browser.
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

        // Full nanopublication links (graph-aware formats)
        add(createLink("trig", type, id, contextId, "trig", false, false));
        add(createLink("trig-txt", type, id, contextId, "trig", true, false));
        add(createLink("jsonld", type, id, contextId, "jsonld", false, false));
        add(createLink("jsonld-txt", type, id, contextId, "jsonld", true, false));
        add(createLink("nq", type, id, contextId, "nq", false, false));
        add(createLink("nq-txt", type, id, contextId, "nq", true, false));
        add(createLink("trix", type, id, contextId, "trix", false, false));
        add(createLink("trix-txt", type, id, contextId, "trix", true, false));

        // Assertions-only links (triple-based formats)
        add(createLink("turtle", type, id, contextId, "turtle", false, true));
        add(createLink("turtle-txt", type, id, contextId, "turtle", true, true));
        add(createLink("a-jsonld", type, id, contextId, "jsonld", false, true));
        add(createLink("a-jsonld-txt", type, id, contextId, "jsonld", true, true));
        add(createLink("nt", type, id, contextId, "nt", false, true));
        add(createLink("nt-txt", type, id, contextId, "nt", true, true));
        add(createLink("rdfxml", type, id, contextId, "rdfxml", false, true));
        add(createLink("rdfxml-txt", type, id, contextId, "rdfxml", true, true));
    }

    private BookmarkablePageLink<Void> createLink(String wicketId, String type, String id, String contextId,
            String format, boolean txt, boolean assertionsOnly) {
        PageParameters params = new PageParameters()
                .set("type", type)
                .set("id", id)
                .set("format", format);
        if (contextId != null) {
            params.set("context", contextId);
        }
        if (txt) {
            params.set("txt", "");
        }
        if (assertionsOnly) {
            params.set("assertions", "");
        }
        return new BookmarkablePageLink<>(wicketId, DownloadRdfPage.class, params);
    }

}
