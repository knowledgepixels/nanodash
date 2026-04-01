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
        this(markupId, baseParams(type, id, contextId));
    }

    /**
     * Creates download links with arbitrary base parameters (e.g. for list page filters).
     *
     * @param markupId   the Wicket markup ID
     * @param baseParams parameters common to all links (type, id, filters, etc.)
     */
    public DownloadRdfLinks(String markupId, PageParameters baseParams) {
        super(markupId);

        // Full nanopublication links (graph-aware formats)
        add(createLink("trig", baseParams, "trig", false, false));
        add(createLink("trig-txt", baseParams, "trig", true, false));
        add(createLink("jsonld", baseParams, "jsonld", false, false));
        add(createLink("jsonld-txt", baseParams, "jsonld", true, false));
        add(createLink("nq", baseParams, "nq", false, false));
        add(createLink("nq-txt", baseParams, "nq", true, false));
        add(createLink("trix", baseParams, "trix", false, false));
        add(createLink("trix-txt", baseParams, "trix", true, false));

        // Assertions-only links (triple-based formats)
        add(createLink("turtle", baseParams, "turtle", false, true));
        add(createLink("turtle-txt", baseParams, "turtle", true, true));
        add(createLink("a-jsonld", baseParams, "jsonld", false, true));
        add(createLink("a-jsonld-txt", baseParams, "jsonld", true, true));
        add(createLink("nt", baseParams, "nt", false, true));
        add(createLink("nt-txt", baseParams, "nt", true, true));
        add(createLink("rdfxml", baseParams, "rdfxml", false, true));
        add(createLink("rdfxml-txt", baseParams, "rdfxml", true, true));
    }

    private static PageParameters baseParams(String type, String id, String contextId) {
        PageParameters params = new PageParameters()
                .set("type", type)
                .set("id", id);
        if (contextId != null) {
            params.set("context", contextId);
        }
        return params;
    }

    private BookmarkablePageLink<Void> createLink(String wicketId, PageParameters baseParams,
            String format, boolean txt, boolean assertionsOnly) {
        PageParameters params = new PageParameters(baseParams)
                .set("format", format);
        if (txt) {
            params.set("txt", "");
        }
        if (assertionsOnly) {
            params.set("assertions", "");
        }
        return new BookmarkablePageLink<>(wicketId, DownloadRdfPage.class, params);
    }

}
