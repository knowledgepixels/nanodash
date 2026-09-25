package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.DiscussionThread;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;

public class QueryResultThread extends QueryResult {

    public static final int MAX_DEPTH = 3;

    public static final int DEFAULT_PAGE_SIZE = 5;

    private final DiscussionThread thread;

    QueryResultThread(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        this.thread = DiscussionThread.of(response);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            label = viewDisplay.getTitle();
        }
        add(new Label("label", label).setVisible(label != null && !label.isEmpty()));
        setOutputMarkupId(true);

        Label stats = new Label("stats", thread.getResponseCount() + " contributions · "
                + thread.getPeopleCount() + (thread.getPeopleCount() == 1 ? " person" : " people"));
        stats.setVisible(!thread.isEmpty());
        add(stats);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Called by the builder rather than from the constructor: every card is
     * built here against the page context (the context id, the part, the pinned
     * ref), and those are set on this component after it is constructed.</p>
     */
    @Override
    public void populateComponent() {
        View view = viewDisplay.getView();
        int pageSize = DEFAULT_PAGE_SIZE;
        if (view != null && view.getPageSize() != null) {
            pageSize = view.getPageSize();
        }

        ThreadNodePanel.ThreadContext context = new ThreadNodePanel.ThreadContext(view, queryRef, contextId,
                partId, postPublishTab, refRoot,
                resourceWithProfile != null ? resourceWithProfile : pageResource,
                pageSize, MAX_DEPTH);

        RepeatingView roots = new RepeatingView("roots");
        for (DiscussionThread.Node root : thread.getRoots()) {
            roots.add(new ThreadNodePanel(roots.newChildId(), context, root, 0));
        }
        WebMarkupContainer threadContainer = new WebMarkupContainer("thread-container");
        threadContainer.setOutputMarkupId(true);
        threadContainer.add(roots);
        threadContainer.add(new Label("no-records", "(nothing found)").setVisible(thread.isEmpty()));
        addOrReplace(threadContainer);
    }

    /**
     * The thread this view is showing.
     *
     * @return the thread, never null
     */
    public DiscussionThread getThread() {
        return thread;
    }

}
