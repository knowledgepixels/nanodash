package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.eclipse.rdf4j.model.Model;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component that asynchronously loads and displays the RDF result of a CONSTRUCT query.
 */
public abstract class RdfResultComponent extends ResultComponent {

    private final QueryRef queryRef;
    private Model model = null;
    private static final Logger logger = LoggerFactory.getLogger(RdfResultComponent.class);

    /**
     * Constructor.
     *
     * @param id       the component id
     * @param queryRef the QueryRef for the CONSTRUCT query
     */
    public RdfResultComponent(String id, QueryRef queryRef) {
        super(id);
        this.queryRef = queryRef;
    }

    @Override
    public Component getLazyLoadComponent(String markupId) {
        while (true) {
            if (!ApiCache.isRunning(queryRef)) {
                try {
                    model = ApiCache.retrieveRdfModelAsync(queryRef);
                    if (model != null) break;
                } catch (Exception ex) {
                    return new Label(markupId, "<span class=\"negative\">API call failed.</span>").setEscapeModelStrings(false);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                logger.error("Interrupted while waiting for RDF response", ex);
            }
        }
        return getRdfResultComponent(markupId, model);
    }

    @Override
    protected boolean isContentReady() {
        return model != null || !ApiCache.isRunning(queryRef);
    }

    /**
     * Implement to return the component that displays the RDF result.
     *
     * @param markupId the markup ID for the component
     * @param model    the RDF model from the CONSTRUCT query
     * @return a Component that displays the RDF model
     */
    public abstract Component getRdfResultComponent(String markupId, Model model);

}
