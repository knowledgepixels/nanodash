package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * Page that displays all nanopublications of a specific type.
 */
public class TypePage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/type";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private IRI typeIri;
    private boolean added = false;
    private static final Logger logger = LoggerFactory.getLogger(TypePage.class);

    /**
     * Constructor for the TypePage.
     *
     * @param parameters Page parameters containing the type IRI.
     */
    public TypePage(final PageParameters parameters) {
        super(parameters);

        if (parameters.get("id") == null) throw new RedirectToUrlException(HomePage.MOUNT_PATH);
        typeIri = Utils.vf.createIRI(parameters.get("id").toString());

        add(new TitleBar("titlebar", this, null));

        final String displayName = Utils.getTypeLabel(typeIri);
        add(new Label("pagetitle", displayName + " (type) | nanodash"));
        add(new Label("type", displayName));

        add(new ExternalLink("urilink", typeIri.stringValue(), typeIri.stringValue()));

        refresh();
    }

    /**
     * <p>hasAutoRefreshEnabled.</p>
     *
     * @return a boolean
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

    private synchronized void refresh() {
        if (added) {
            remove("nanopubs");
        }
        added = true;
        final Map<String, String> params = new HashMap<>();
        params.put("type", typeIri.stringValue());
        final String queryName = "get-latest-nanopubs-by-type";
        ApiResponse cachedResponse = ApiCache.retrieveResponse(queryName, params);
        if (cachedResponse != null) {
            add(NanopubResults.fromApiResponse("nanopubs", cachedResponse));
        } else {
            add(new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

                private static final long serialVersionUID = 1L;

                @Override
                public NanopubResults getLazyLoadComponent(String markupId) {
                    ApiResponse r = null;
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            logger.error("Interrupted while waiting for API response", ex);
                        }
                        if (!ApiCache.isRunning(queryName, params)) {
                            r = ApiCache.retrieveResponse(queryName, params);
                            if (r != null) break;
                        }
                    }
                    return NanopubResults.fromApiResponse(markupId, r);
                }

                @Override
                protected void onContentLoaded(NanopubResults content, Optional<AjaxRequestTarget> target) {
                    super.onContentLoaded(content, target);
                    if (target.isPresent()) {
                        target.get().appendJavaScript("updateElements();");
                    }
                }

            });
        }
    }

}
