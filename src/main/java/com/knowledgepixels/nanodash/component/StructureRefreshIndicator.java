package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.io.Serial;
import java.time.Duration;

/**
 * The spinner beside a page's title menu, shown while the page structure — which views the
 * resource shows — is being recalculated after a publication that can change it (issue
 * #622). The views themselves each show their own spinner while their query is re-run
 * ({@link RefreshingResultPanel}); this one is about the page as a whole, whose recalculation
 * is otherwise invisible now that it no longer blanks the content out.
 * <p>
 * It polls on its own rather than being driven by {@link RefreshingStructurePanel}: the
 * structure is not always rendered through that panel (an empty one still goes down the
 * pages' lazy path), and a title that sits outside the content area has no other way of
 * hearing that the refresh has landed. The timer is only installed when a refresh is
 * actually pending, so an ordinary page render carries no polling at all.
 */
public class StructureRefreshIndicator extends WebMarkupContainer {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    // How long to keep the spinner up before giving up on the refresh; the same budget the
    // refreshing view panels get.
    private static final long TIMEOUT_MS = 60_000;

    // Held by id rather than by reference: the resources are process-wide singletons, and a
    // copy revived from the page store would carry its own stale refresh flags.
    private final String resourceId;

    private final long deadline;

    /**
     * @param id       the Wicket markup id
     * @param resource the resource whose structure is being watched, or null for none
     */
    public StructureRefreshIndicator(String id, AbstractResourceWithProfile resource) {
        super(id);
        this.resourceId = resource == null ? null : resource.getId();
        this.deadline = System.currentTimeMillis() + TIMEOUT_MS;
        setOutputMarkupPlaceholderTag(true);
        if (isRefreshing()) add(new IndicatorPollTimer());
    }

    private boolean isRefreshing() {
        if (resourceId == null) return false;
        AbstractResourceWithProfile resource = AbstractResourceWithProfile.get(resourceId);
        return resource != null && resource.isStructureRefreshPending();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onConfigure() {
        super.onConfigure();
        setVisible(isRefreshing());
    }

    /**
     * Polls for the end of the refresh and takes the spinner away when it lands.
     */
    private class IndicatorPollTimer extends AbstractAjaxTimerBehavior {

        @Serial
        private static final long serialVersionUID = 1L;

        IndicatorPollTimer() {
            super(POLL_INTERVAL);
        }

        @Override
        protected void onTimer(AjaxRequestTarget target) {
            if (isRefreshing() && System.currentTimeMillis() <= deadline) return;
            setVisible(false);
            target.add(StructureRefreshIndicator.this);
            stop(target);
        }

    }

}
