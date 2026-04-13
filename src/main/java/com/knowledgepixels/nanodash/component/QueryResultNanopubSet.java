package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.FilteredQueryResultDataProvider;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.QueryResultDataProvider;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.menu.ViewDisplayMenu;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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

        viewSelector.add(new AjaxLink<>("listEnabler") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.LIST);
                logger.info("ListEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        viewSelector.add(new AjaxLink<>("gridEnabler") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.GRID);
                logger.info("GridEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        viewSelector.add(new Label("np"));
        add(viewSelector);
        showViewDisplayMenu = false; // handled in populateComponent() inside viewSelector

        String titleLabel = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            titleLabel = viewDisplay.getTitle();
        }
        add(new Label("title", titleLabel));

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && nanopubsContainer != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    nanopubsContainer.addOrReplace(buildNanopubResults());
                    target.add(nanopubsContainer);
                }
            }
        });
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
        add(nanopubsContainer);

        if (viewDisplay.getNanopubId() != null) {
            viewSelector.addOrReplace(new ViewDisplayMenu("np", viewDisplay, queryRef, pageResource));
        } else {
            viewSelector.addOrReplace(new Label("np").setVisible(false));
        }
    }

    private NanopubResults buildNanopubResults() {
        NanopubResults nanopubResults = NanopubResults.fromApiResponse("nanopubs", filteredDataProvider.getFilteredData(), itemsPerPage);
        nanopubResults.add(AttributeAppender.append("class", NanodashSession.get().getNanopubResultsViewMode().getValue()));
        return nanopubResults;
    }

    /**
     * Sets the visibility of the title.
     *
     * @param hasTitle true to show the title, false to hide it
     */
    public void setTitleVisible(boolean hasTitle) {
        this.get("title").setVisible(hasTitle);
        if (!hasTitle) {
            viewSelector.add(AttributeAppender.append("class", " no-title"));
        }
    }

}
