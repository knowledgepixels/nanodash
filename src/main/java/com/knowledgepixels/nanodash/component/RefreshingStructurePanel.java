package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;

import java.io.Serial;
import java.time.Duration;
import java.util.Objects;

/**
 * Keeps a resource's currently loaded page structure (its {@link ViewList} of view
 * displays) on screen while a refresh of that structure is in flight, and swaps in the
 * rebuilt one once the refreshed data has landed — but only if the structure actually
 * changed (issue #622).
 * <p>
 * The counterpart of {@link RefreshingResultPanel} one level up: that one keeps a single
 * view's outdated results visible while its query is re-fetched, this one keeps the set of
 * views itself visible while the resource's view-display query is re-fetched. Blanking the
 * whole content area out instead — which is what {@code forceRefresh} used to do by
 * invalidating the loaded data — also destroyed the in-place refresh of the very view the
 * user had just published from, since every view panel was rebuilt cold afterwards.
 */
public class RefreshingStructurePanel extends Panel {

    private static final String CONTENT_ID = "content";

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    // How long to keep waiting for the refreshed structure before settling on the one
    // shown; the same budget the refreshing result panels get.
    private static final long REFRESH_TIMEOUT_MS = 60_000;

    /**
     * Wraps the content in a refreshing panel while the resource's structure refresh is
     * pending, and returns the content itself otherwise, so an ordinary render neither
     * gains a wrapper nor a polling timer.
     *
     * @param id       the Wicket markup id
     * @param resource the resource whose structure the content renders
     * @param factory  builds the content component for a markup id
     * @return the content component, or a refreshing panel around it
     */
    public static Component of(String id, AbstractResourceWithProfile resource, LazyContentPanel.ContentFactory factory) {
        if (resource == null || !resource.isStructureRefreshPending()) return factory.create(id);
        return new RefreshingStructurePanel(id, resource, factory);
    }

    private final String resourceId;
    private final LazyContentPanel.ContentFactory factory;
    private final String shownSignature;
    private final long deadline;

    private RefreshingStructurePanel(String id, AbstractResourceWithProfile resource, LazyContentPanel.ContentFactory factory) {
        super(id);
        // Held by id rather than by reference: the resources are process-wide singletons,
        // and a copy revived from the page store would carry its own stale refresh flags.
        this.resourceId = resource.getId();
        this.factory = factory;
        this.shownSignature = resource.getStructureSignature();
        this.deadline = System.currentTimeMillis() + REFRESH_TIMEOUT_MS;
        setOutputMarkupId(true);
        add(factory.create(CONTENT_ID));
        add(new StructurePollTimer());
    }

    private void poll(AjaxRequestTarget target, AbstractAjaxTimerBehavior timer) {
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(resourceId);
        if (resource == null) {
            timer.stop(target);
            return;
        }
        if (resource.isStructureRefreshPending()) {
            if (System.currentTimeMillis() > deadline) timer.stop(target);
            return;
        }
        timer.stop(target);
        if (Objects.equals(shownSignature, resource.getStructureSignature())) {
            // The refresh left the page structure as it is, so there is nothing worth
            // rebuilding — and rebuilding anyway would throw away whatever the user is
            // doing in the views (a typed filter, an open menu, the current table page).
            return;
        }
        addOrReplace(factory.create(CONTENT_ID));
        target.add(this);
    }

    /**
     * Polls for the refreshed structure, in the manner of Wicket's own
     * {@code AjaxLazyLoadPanel} timer: one per panel, removing itself once it is done.
     */
    private class StructurePollTimer extends AbstractAjaxTimerBehavior {

        @Serial
        private static final long serialVersionUID = 1L;

        StructurePollTimer() {
            super(POLL_INTERVAL);
        }

        @Override
        protected void onTimer(AjaxRequestTarget target) {
            poll(target, this);
        }

    }

}
