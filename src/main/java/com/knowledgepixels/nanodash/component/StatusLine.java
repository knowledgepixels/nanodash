package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.page.ExplorePage;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A component that displays the status of a nanopublication.
 * <p>
 * If the registry has not yet indexed the nanopublication at first render, the
 * panel polls the cache every few seconds and updates itself in place once
 * verification succeeds — so users don't have to manually reload the page
 * after publishing.
 */
public class StatusLine extends Panel {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final long POLL_TIMEOUT_MS = 2 * 60 * 1000L;

    /**
     * Creates a new StatusLine component.
     *
     * @param markupId the Wicket markup ID for this component
     * @param npId     the nanopublication ID to check for newer versions
     * @return a new StatusLine component
     */
    public static Component createComponent(String markupId, String npId) {
        ApiResultComponent c = new ApiResultComponent(markupId, new QueryRef(QueryApiAccess.GET_NEWER_VERSIONS_OF_NP, "np", npId)) {

            @Override
            public Component getApiResultComponent(String markupId, ApiResponse response) {
                return new StatusLine(markupId, npId, response);
            }

        };
        c.setWaitComponentHtml("<p><strong>Status:</strong> " + ApiResultComponent.getWaitIconHtml() + "</p>");
        return c;
    }

    private final String npId;
    private final long pollStart = System.currentTimeMillis();
    private String statusText;
    private List<String> links = List.of();
    private boolean verified = false;

    /**
     * Constructs a StatusLine component with the given markup ID, nanopublication ID, and API response.
     *
     * @param markupId the Wicket markup ID for this component
     * @param npId     the nanopublication ID to check for newer versions
     * @param response the API response containing data about newer versions or retractions
     */
    StatusLine(String markupId, String npId, ApiResponse response) {
        super(markupId);
        this.npId = npId;
        setOutputMarkupId(true);
        applyResponse(response);

        add(new Label("statusText", (IModel<String>) () -> statusText).setEscapeModelStrings(false));
        add(new ListView<String>("linkList", (IModel<List<String>>) () -> links) {
            @Override
            protected void populateItem(ListItem<String> item) {
                String id = item.getModelObject();
                String shortLabel = TrustyUriUtils.getArtifactCode(id).substring(0, 10);
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("npLink",
                        ExplorePage.class,
                        new PageParameters().add("id", id));
                link.add(new Label("npLabel", shortLabel));
                item.add(link);
            }
        });

        if (!verified) {
            final QueryRef queryRef = new QueryRef(QueryApiAccess.GET_NEWER_VERSIONS_OF_NP, "np", npId);
            add(new AjaxSelfUpdatingTimerBehavior(POLL_INTERVAL) {
                @Override
                protected void onPostProcessTarget(AjaxRequestTarget target) {
                    ApiCache.clearCache(queryRef, 0);
                    ApiResponse fresh = ApiCache.retrieveResponseAsync(queryRef);
                    if (fresh != null) {
                        applyResponse(fresh);
                    }
                    if (verified || System.currentTimeMillis() - pollStart > POLL_TIMEOUT_MS) {
                        stop(target);
                    }
                }
            });
        }
    }

    private void applyResponse(ApiResponse response) {
        List<String> latest = new ArrayList<>();
        List<String> retractions = new ArrayList<>();
        for (ApiResponseEntry e : response.getData()) {
            String newerVersion = e.get("newerVersion");
            String retractedBy = e.get("retractedBy");
            String supersededBy = e.get("supersededBy");
            if (retractedBy.isEmpty() && supersededBy.isEmpty()) {
                latest.add(newerVersion);
            } else if (!retractedBy.isEmpty() && supersededBy.isEmpty()) {
                retractions.add(retractedBy);
            }
        }

        if (latest.isEmpty() && retractions.isEmpty()) {
            statusText = "<em>This nanopublication doesn't seem to be properly published (yet). This can take a minute or two for new nanopublications.</em>";
            links = List.of();
            verified = false;
        } else if (!latest.isEmpty()) {
            if (latest.size() == 1 && latest.getFirst().equals(npId)) {
                statusText = "This is the latest version.";
                links = List.of();
            } else if (latest.size() == 1) {
                statusText = "This nanopublication has a <strong>newer version</strong>:";
                links = latest;
            } else {
                statusText = "This nanopublication has <strong>newer versions</strong>:";
                links = latest;
            }
            verified = true;
        } else {
            statusText = "This nanopublication has been <strong>retracted</strong>:";
            links = retractions;
            verified = true;
        }
    }

}
