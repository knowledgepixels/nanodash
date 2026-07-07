package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.FilteredQueryResultDataProvider;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.QueryResultDataProvider;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.menu.ViewDisplayMenu;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultNanopubSet extends QueryResult {

    private static final Logger logger = LoggerFactory.getLogger(QueryResultNanopubSet.class);
    private final WebMarkupContainer viewSelector;
    private final long itemsPerPage;
    private FilteredQueryResultDataProvider filteredDataProvider;
    private Model<String> filterModel = Model.of("");
    private WebMarkupContainer nanopubsContainer;

    // An empty title override (ViewDisplay.withTitle("")) hides the title row; kept as
    // a flag so the builder's later setTitleVisible(true) doesn't re-show an empty h4.
    private boolean emptyTitle = false;

    /**
     * Constructor for QueryResultList.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultNanopubSet(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay, long itemsPerPage) {
        super(markupId, queryRef, response, viewDisplay);
        this.itemsPerPage = itemsPerPage;

        logger.info("Rendering {} with '{}' mode.", this.getClass().getName(), NanodashSession.get().getNanopubResultsViewMode().getValue());

        viewSelector = new WebMarkupContainer("viewSelector");
        viewSelector.setOutputMarkupId(true);

        // The tiled (grid) view mode is deactivated for now: this view always renders
        // as a list (see buildNanopubResults), so the list/grid toggle is omitted.
        viewSelector.add(new Label("np"));
        add(viewSelector);
        showViewDisplayMenu = false; // handled in populateComponent() inside viewSelector

        String titleLabel = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            titleLabel = viewDisplay.getTitle();
        }
        emptyTitle = (titleLabel == null || titleLabel.isEmpty());
        add(new Label("title", titleLabel));

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new FilterUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && nanopubsContainer != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    nanopubsContainer.addOrReplace(buildNanopubResults());
                    target.add(nanopubsContainer);
                }
            }
        });
        filterField.setVisible(!fitsOnFirstPage());
        viewSelector.add(filterField);

        setOutputMarkupId(true);
    }

    @Override
    protected void populateComponent() {
        logger.info("Populating the component with nanopub results.");
        filteredDataProvider = new FilteredQueryResultDataProvider(new QueryResultDataProvider(response.getData()), response);

        nanopubsContainer = new WebMarkupContainer("nanopubs-container");
        nanopubsContainer.setOutputMarkupId(true);
        nanopubsContainer.add(buildNanopubResults());
        Label noRecordsLabel = new Label("no-records", "(nothing found)") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(filteredDataProvider.size() == 0);
            }
        };
        nanopubsContainer.add(noRecordsLabel);
        add(nanopubsContainer);

        if (viewDisplay.getNanopubId() != null || !getMenuActions().isEmpty()) {
            viewSelector.addOrReplace(new ViewDisplayMenu("np", viewDisplay, queryRef, pageResource, getMenuActions()));
        } else {
            viewSelector.addOrReplace(new Label("np").setVisible(false));
        }
    }

    private NanopubResults buildNanopubResults() {
        NanopubResults nanopubResults = NanopubResults.fromApiResponse("nanopubs", filteredDataProvider.getFilteredData(), itemsPerPage);
        // Tiled (grid) mode deactivated for now — always render this view as a list,
        // regardless of the session's view-mode setting.
        nanopubResults.add(AttributeAppender.append("class", NanopubResults.ViewMode.LIST.getValue()));
        // Hide the (empty) results container when there is nothing to show, so the
        // "(nothing found)" note sits directly below the header instead of being pushed
        // down by the flex-container's padding.
        nanopubResults.setVisible(!filteredDataProvider.getFilteredData().isEmpty());
        return nanopubResults;
    }

    /**
     * Sets the visibility of the title.
     *
     * @param hasTitle true to show the title, false to hide it
     */
    public void setTitleVisible(boolean hasTitle) {
        boolean visible = hasTitle && !emptyTitle;
        this.get("title").setVisible(visible);
        if (!visible) {
            viewSelector.add(AttributeAppender.append("class", " no-title"));
        }
    }

}
