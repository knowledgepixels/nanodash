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

        add(createLink("trig", type, id, contextId, "trig", false));
        add(createLink("trig-txt", type, id, contextId, "trig", true));
        add(createLink("jsonld", type, id, contextId, "jsonld", false));
        add(createLink("jsonld-txt", type, id, contextId, "jsonld", true));
        add(createLink("nq", type, id, contextId, "nq", false));
        add(createLink("nq-txt", type, id, contextId, "nq", true));
        add(createLink("trix", type, id, contextId, "trix", false));
        add(createLink("trix-txt", type, id, contextId, "trix", true));
    }

    private BookmarkablePageLink<Void> createLink(String wicketId, String type, String id, String contextId, String format, boolean txt) {
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
        return new BookmarkablePageLink<>(wicketId, DownloadRdfPage.class, params);
    }

}
