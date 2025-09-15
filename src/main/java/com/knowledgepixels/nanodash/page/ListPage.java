package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryRef;
import com.knowledgepixels.nanodash.component.NanopubResults;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.nanopub.extra.services.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A page that shows a list of nanopublications filtered by type, public key, and time range.
 */
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
    private final List<IRI> types = new ArrayList<>();
    private final List<String> pubKeys = new ArrayList<>();
    private String startTime = "";
    private String endTime = "";
    private static ViewMode currentViewMode = null;

    private enum ViewMode {
        LIST("list-view"),
        GRID("grid-view");

        private final String value;

        ViewMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Constructor for ListPage.
     *
     * @param parameters Page parameters containing filters like types, pubKeys, startTime, and endTime.
     */
    public ListPage(final PageParameters parameters) {
        super(parameters);

        if (currentViewMode == null) {
            currentViewMode = ViewMode.GRID;
        }
        logger.info("Rendering ListPage with '{}' mode.", currentViewMode.getValue());

        add(new AjaxLink<>("listEnabler") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentViewMode = ViewMode.LIST;
                logger.info("Switched to '{}' mode", currentViewMode.getValue());
            }
        });

        add(new AjaxLink<>("gridEnabler") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                currentViewMode = ViewMode.GRID;
                logger.info("Switched to '{}' mode", currentViewMode.getValue());
            }
        });

        // TODO the query works with multiple types, but the UI does not yet support that so we just assume one type is mandatory and we show the first one only for now
        if (parameters.get("types").isNull() || parameters.get("types").isEmpty()) {
            throw new RedirectToUrlException(HomePage.MOUNT_PATH);
        } else {
            Arrays.stream(parameters.get("types").toString().split(","))
                    .toList()
                    .forEach(type -> types.add(Values.iri(type)));
        }

        if (!parameters.get("pubKeys").isNull() && !parameters.get("pubKeys").isEmpty()) {
            pubKeys.addAll(Arrays.stream(parameters.get("pubKeys").toString().split(",")).toList());
        }

        if (!parameters.get("startTime").isNull() && !parameters.get("startTime").isEmpty()) {
            startTime = parameters.get("startTime").toString();
        }

        if (!parameters.get("endTime").isNull() && !parameters.get("endTime").isEmpty()) {
            endTime = parameters.get("endTime").toString();
        }

        add(new TitleBar("titlebar", this, null));
        add(new Label("pagetitle", "Nanopublication list | nanodash"));

        // TODO show multiple types. Currently we just show the first one and assume at least one is present
        add(new ExternalLink("typeUri", types.getFirst().stringValue(), types.getFirst().stringValue()));
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
        if (!types.isEmpty()) {
            params.put("types", types.stream().map(IRI::stringValue).collect(Collectors.joining(" ")));
        }
        if (!pubKeys.isEmpty()) {
            params.put("pubkeys", String.join(" ", pubKeys));
        }
        if (!startTime.isBlank()) {
            params.put("starttime", startTime);
        }
        if (!endTime.isBlank()) {
            params.put("endtime", endTime);
        }
        final QueryRef queryRef = new QueryRef("get-filtered-nanopub-list", params);
        ApiResponse cachedResponse = ApiCache.retrieveResponse(queryRef);
        if (cachedResponse != null) {
            NanopubResults cachedResults = NanopubResults.fromApiResponse("nanopubs", cachedResponse);
            cachedResults.add(AttributeAppender.append("class", currentViewMode.getValue()));
            add(cachedResults);
        } else {
            AjaxLazyLoadPanel<NanopubResults> results = new AjaxLazyLoadPanel<NanopubResults>("nanopubs") {

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

            };
            results.add(AttributeAppender.append("class", currentViewMode.getValue()));
            add(results);
        }
    }

}
