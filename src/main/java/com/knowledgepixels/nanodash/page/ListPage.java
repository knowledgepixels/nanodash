package com.knowledgepixels.nanodash.page;

import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;

public class ListPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/list";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private boolean added = false;
    private static final Logger logger = LoggerFactory.getLogger(ListPage.class);

    public ListPage(final PageParameters parameters) {
        super(parameters);

        if (parameters.get("id") == null) throw new RedirectToUrlException(HomePage.MOUNT_PATH);

        add(new TitleBar("titlebar", this, null));

        add(new Label("pagetitle", "Nanopublication list | nanodash"));

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
        final QueryRef queryRef = new QueryRef("get-most-recent-nanopubs");
        ApiResponse cachedResponse = ApiCache.retrieveResponse(queryRef);
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
                        if (!ApiCache.isRunning(queryRef)) {
                            r = ApiCache.retrieveResponse(queryRef);
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
