package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.ApiCache;
import com.knowledgepixels.nanodash.QueryResult;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * Shows a query's previous, outdated results while a refresh for it is in flight, with a
 * spinner next to them (issue #599). Beats the plain loading spinner whenever there is
 * anything cached at all: a browser reload, a cache cleared after publishing, or a cached
 * entry past its maximum age all leave the user looking at content that is merely a bit
 * behind, so there is no reason to blank it out first.
 * <p>
 * When the refresh lands, the panel compares it to what is on screen and only swaps the
 * content in if the results actually changed — an unchanged refresh just takes the spinner
 * away, leaving whatever the user is doing in the panel (a typed filter, an open menu, the
 * current table page) untouched.
 * <p>
 * Polling is done by a single {@link RefreshPollTimer} per page, in the manner of Wicket's
 * own {@code AjaxLazyLoadPanel}: all panels on the page piggyback on it, and it stops as
 * soon as none of them is waiting anymore.
 */
public class RefreshingResultPanel extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(RefreshingResultPanel.class);

    private static final String CONTENT_ID = "content";

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    // How long to wait for the refresh before settling on the outdated content: the same
    // budget the cold-start spinner gets in ApiResultComponent.
    private static final long REFRESH_TIMEOUT_MS = 60_000;

    private final QueryRef queryRef;
    private final ApiResultRenderer renderer;

    // Fingerprint of the results currently on screen, to tell a refresh that changed
    // something from one that did not. Null when it cannot be computed (RDF responses),
    // which makes every refresh count as a change.
    private final String shownDigest;

    private final long deadline;
    private final WebMarkupContainer spinner;

    private boolean refreshing = true;

    /**
     * @param id            the Wicket markup id
     * @param queryRef      the query being refreshed
     * @param staleResponse the outdated results to show meanwhile (not null)
     * @param renderer      builds the component showing a set of results
     */
    public RefreshingResultPanel(String id, QueryRef queryRef, ApiResponse staleResponse, ApiResultRenderer renderer) {
        super(id);
        this.queryRef = queryRef;
        this.renderer = renderer;
        this.shownDigest = digest(staleResponse);
        this.deadline = System.currentTimeMillis() + REFRESH_TIMEOUT_MS;
        setOutputMarkupId(true);
        add(showSpinnerOn(renderer.render(CONTENT_ID, staleResponse)));
        // Fallback for content that has no title row of its own to put a spinner in. The
        // view components all do, so this normally stays hidden.
        spinner = new WebMarkupContainer("spinner");
        spinner.setOutputMarkupPlaceholderTag(true);
        spinner.setVisible(false);
        add(spinner);
    }

    // Turns on the spinner the view shows beside its own title, and reports whether the
    // content actually has one.
    private Component showSpinnerOn(Component content) {
        if (content instanceof QueryResult view) {
            view.setRefreshing(true);
            hasOwnIndicator = true;
        }
        return content;
    }

    private boolean hasOwnIndicator = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        // Marks the panel as updating, for the client-side indicator in nanodash.js (which
        // then leaves this panel alone rather than adding a second spinner).
        if (refreshing) tag.append("class", "view-refreshing", " ");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigure() {
        super.onConfigure();
        // Only the fallback needs its own spinner: the view components show theirs in their
        // title row.
        spinner.setVisible(refreshing && !hasOwnIndicator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (refreshing) {
            RefreshPollTimer.install(getPage());
        }
    }

    /**
     * Checks whether this panel's refresh has landed, and acts on it. Called by the page's
     * {@link RefreshPollTimer} once per tick.
     *
     * @param target the AJAX target of the polling request
     * @return true if this panel is still waiting for its refresh
     */
    boolean poll(AjaxRequestTarget target) {
        if (!refreshing) return false;
        ApiResponse fresh;
        try {
            fresh = ApiCache.retrieveResponseAsync(queryRef);
        } catch (Exception ex) {
            // The query is failing; the outdated content is all we are going to get.
            logger.error("Failed to refresh {}: {}", queryRef.getAsUrlString(), ex.getMessage());
            settle(target);
            return false;
        }
        if (fresh == null) {
            if (System.currentTimeMillis() < deadline) return true;
            logger.warn("Timed out refreshing {}; keeping the outdated content", queryRef.getAsUrlString());
            settle(target);
            return false;
        }
        if (shownDigest != null && shownDigest.equals(digest(fresh))) {
            // Nothing changed, so nothing is worth interrupting the user for.
            settle(target);
            return false;
        }
        refreshing = false;
        spinner.setVisible(false);
        // The replacement renders without the spinner, since it is not refreshing anymore.
        addOrReplace(renderer.render(CONTENT_ID, fresh));
        target.add(this);
        return false;
    }

    // Stops waiting without touching the content: turns the spinner off and repaints just
    // that, so whatever the user is doing in the panel is left alone.
    private void settle(AjaxRequestTarget target) {
        refreshing = false;
        Component content = get(CONTENT_ID);
        if (content instanceof QueryResult view) {
            view.setRefreshing(false);
            target.add(view.getRefreshIndicator());
        }
        spinner.setVisible(false);
        target.add(spinner);
    }

    /**
     * Fingerprints a response's contents, so two responses can be compared without keeping
     * the older one around.
     *
     * @param response the response to fingerprint
     * @return the fingerprint, or null if the response cannot be fingerprinted
     */
    private static String digest(ApiResponse response) {
        if (response == null || response.isRdfResponse()) return null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is required of every Java platform, so this cannot happen; treating
            // the response as unfingerprintable just means always swapping in the refresh.
            logger.error("SHA-256 not available", ex);
            return null;
        }
        if (response.getHeader() != null) {
            for (String column : response.getHeader()) {
                update(md, column);
            }
        }
        List<ApiResponseEntry> data = response.getData();
        if (data != null) {
            for (ApiResponseEntry entry : data) {
                // The keys come as a set, so fix an order before digesting them.
                List<String> keys = new ArrayList<>(entry.getKeys());
                Collections.sort(keys);
                for (String key : keys) {
                    update(md, key);
                    update(md, entry.get(key));
                }
                md.update((byte) 2);
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    // Digests one value, keeping the boundary between values explicit so that neighbouring
    // values cannot be shifted into each other without changing the digest.
    private static void update(MessageDigest md, String value) {
        if (value != null) md.update(value.getBytes(StandardCharsets.UTF_8));
        md.update((byte) 1);
    }

    /**
     * The page-wide timer polling every {@link RefreshingResultPanel} on the page, modelled
     * on Wicket's {@code AjaxLazyLoadPanel.AjaxLazyLoadTimer}: one timer for all panels,
     * removing itself once every panel has settled.
     */
    public static class RefreshPollTimer extends AbstractAjaxTimerBehavior {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Adds the timer to a page unless it already has one.
         *
         * @param page the page holding the refreshing panels
         */
        static void install(Page page) {
            if (!page.getBehaviors(RefreshPollTimer.class).isEmpty()) return;
            RefreshPollTimer timer = new RefreshPollTimer();
            page.add(timer);
            RequestCycle rc = RequestCycle.get();
            if (rc != null) {
                // Panels appearing within an AJAX response (e.g. a lazily loaded tab body)
                // do not render the page, so the timer has to be started on the target.
                rc.find(AjaxRequestTarget.class).ifPresent(timer::restart);
            }
        }

        /**
         * Constructor.
         */
        public RefreshPollTimer() {
            super(POLL_INTERVAL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onTimer(AjaxRequestTarget target) {
            // Collected first and polled after: polling swaps components in, and the
            // component tree should not be rewritten while it is being walked.
            final List<RefreshingResultPanel> panels = new ArrayList<>();
            getComponent().getPage().visitChildren(RefreshingResultPanel.class,
                    new IVisitor<RefreshingResultPanel, Void>() {
                        @Override
                        public void component(RefreshingResultPanel panel, IVisit<Void> visit) {
                            if (panel.isVisibleInHierarchy()) panels.add(panel);
                        }
                    });
            boolean pending = false;
            for (RefreshingResultPanel panel : panels) {
                if (panel.poll(target)) pending = true;
            }
            if (!pending) {
                stop(target);
                getComponent().remove(this);
            }
        }

    }

}
