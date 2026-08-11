package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.GovernedVersions;
import com.knowledgepixels.nanodash.NavigationContext;
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
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>
 * Newer versions are found in two ways: via the {@code npx:supersedes} chain
 * (and retractions) with the {@link QueryApiAccess#GET_NEWER_VERSIONS_OF_NP}
 * query, and — for nanopublications embedding a space-governed definition — via
 * the {@code (kind, space)} pair the definition floats within (issue #584). A
 * governed new version carries no {@code npx:supersedes}, so the first query
 * alone reports such a nanopublication as the latest version.
 */
public class StatusLine extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(StatusLine.class);

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final long POLL_TIMEOUT_MS = 2 * 60 * 1000L;

    private static final String LATEST_TEXT = "This is the latest version.";
    private static final String NEWER_VERSION_TEXT = "This nanopublication has a <strong>newer version</strong>:";

    /**
     * Creates a new StatusLine component.
     *
     * @param markupId the Wicket markup ID for this component
     * @param np       the nanopublication to check for newer versions
     * @return a new StatusLine component
     */
    public static Component createComponent(String markupId, Nanopub np) {
        final String npId = np.getUri().stringValue();
        final GovernedVersions.GovernedRef governedRef = GovernedVersions.findGovernedRef(np);
        ApiResultComponent c = new ApiResultComponent(markupId, new QueryRef(QueryApiAccess.GET_NEWER_VERSIONS_OF_NP, "np", npId)) {

            @Override
            public Component getApiResultComponent(String markupId, ApiResponse response) {
                return new StatusLine(markupId, npId, response, governedRef);
            }

        };
        c.setWaitComponentHtml("<p><strong>Status:</strong> " + ApiResultComponent.getWaitIconHtml() + "</p>");
        return c;
    }

    private final String npId;
    private final GovernedVersions.GovernedRef governedRef;
    private final long pollStart = System.currentTimeMillis();
    private String statusText;
    private List<String> links = List.of();
    private boolean verified = false;
    private boolean latestBySupersedes = false;

    /**
     * Constructs a StatusLine component with the given markup ID, nanopublication ID, and API response.
     *
     * @param markupId    the Wicket markup ID for this component
     * @param npId        the nanopublication ID to check for newer versions
     * @param response    the API response containing data about newer versions or retractions
     * @param governedRef the kind/space pair of the space-governed definition this
     *                    nanopublication embeds, or null if it embeds none
     */
    StatusLine(String markupId, String npId, ApiResponse response, GovernedVersions.GovernedRef governedRef) {
        super(markupId);
        this.npId = npId;
        this.governedRef = governedRef;
        setOutputMarkupId(true);
        // Constructed from ApiResultComponent.getLazyLoadComponent, which already
        // waits for its own query off the initial page render, so the governed
        // lookup may block here too.
        applyResponse(response, true);

        add(new Label("statusText", (IModel<String>) () -> statusText).setEscapeModelStrings(false));
        add(new ListView<String>("linkList", (IModel<List<String>>) () -> links) {
            @Override
            protected void populateItem(ListItem<String> item) {
                String id = item.getModelObject();
                String shortLabel = TrustyUriUtils.getArtifactCode(id).substring(0, 10);
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("npLink",
                        ExplorePage.class,
                        new PageParameters().add("id", id));
                // Following a version link shouldn't drop out of the space/user/resource
                // the nanopublication was reached under.
                link.add(NavigationContext.pageContextFallback());
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
                        // On the Wicket request thread, so the governed lookup must
                        // not block; a just-published version is the latest one
                        // anyway, and a later page load resolves it either way.
                        applyResponse(fresh, false);
                    }
                    if (verified || System.currentTimeMillis() - pollStart > POLL_TIMEOUT_MS) {
                        stop(target);
                    }
                }
            });
        }
    }

    private void applyResponse(ApiResponse response, boolean mayBlock) {
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

        latestBySupersedes = false;
        if (latest.isEmpty() && retractions.isEmpty()) {
            statusText = "<em>This nanopublication doesn't seem to be properly published (yet). This can take a minute or two for new nanopublications.</em>";
            links = List.of();
            verified = false;
        } else if (!latest.isEmpty()) {
            if (latest.size() == 1 && latest.getFirst().equals(npId)) {
                statusText = LATEST_TEXT;
                links = List.of();
                latestBySupersedes = true;
            } else if (latest.size() == 1) {
                statusText = NEWER_VERSION_TEXT;
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
        applyGovernedOverride(mayBlock);
    }

    /**
     * Overrides the "latest version" verdict of the supersedes-based query when
     * this nanopublication embeds a space-governed definition that has since
     * floated on to a newer version. Only that verdict is overridden: the
     * not-yet-indexed and retracted states keep their precedence.
     * <p>
     * The governed query already skips retracted versions and only counts
     * versions signed by a current member+ of the governing space, so an empty
     * result means "no valid floating candidate" — the pinned version stands and
     * this nanopublication really is the current one.
     *
     * @param mayBlock whether the caller's thread may wait for the lookup; when
     *                 false, an uncached result leaves the supersedes-based
     *                 verdict in place
     */
    private void applyGovernedOverride(boolean mayBlock) {
        if (governedRef == null || !latestBySupersedes) return;
        ApiResponse response;
        try {
            QueryRef queryRef = GovernedVersions.getQueryRef(governedRef);
            response = mayBlock ? ApiCache.retrieveResponseSync(queryRef, false) : ApiCache.retrieveResponseAsync(queryRef);
        } catch (Exception ex) {
            // The lookup only refines the supersedes-based verdict; on failure
            // that verdict stands rather than the status line breaking.
            logger.error("Error resolving governed version for nanopub: {}", npId, ex);
            return;
        }
        if (response == null) return;
        String governedNpId = GovernedVersions.getVersionNanopubIri(response);
        if (governedNpId != null && !governedNpId.equals(npId)) {
            statusText = NEWER_VERSION_TEXT;
            links = new ArrayList<>(List.of(governedNpId));
        }
    }

}
