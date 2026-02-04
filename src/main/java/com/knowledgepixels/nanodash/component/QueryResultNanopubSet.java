package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultNanopubSet extends QueryResult {

    private static final Logger logger = LoggerFactory.getLogger(QueryResultNanopubSet.class);

    /**
     * Constructor for QueryResultList.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultNanopubSet(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        logger.info("Rendering {} with '{}' mode.", this.getClass().getName(), NanodashSession.get().getNanopubResultsViewMode().getValue());
        add(new AjaxLink<>("listEnabler") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.LIST);
                logger.info("ListEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        add(new AjaxLink<>("gridEnabler") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                NanodashSession.get().setNanopubResultsViewMode(NanopubResults.ViewMode.GRID);
                logger.info("GridEnabler -- Switched to '{}' mode", NanodashSession.get().getNanopubResultsViewMode().getValue());
            }
        });

        String titleLabel = grlcQuery.getLabel();
        if (viewDisplay.getView().getTitle() != null) {
            titleLabel = viewDisplay.getView().getTitle();
        }
        add(new Label("title", titleLabel));
        add(AttributeModifier.remove("class"));
        setOutputMarkupId(true);
    }

    @Override
    protected void populateComponent() {
        logger.info("Populating the component with nanopub results.");
        NanopubResults nanopubResults = NanopubResults.fromApiResponse("nanopubs", response, 10);
        nanopubResults.add(AttributeAppender.append("class", NanodashSession.get().getNanopubResultsViewMode().getValue()));
        add(nanopubResults);

        if (viewDisplay.getNanopubId() != null) {
            add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", viewDisplay.getNanopubId())));
        } else {
            add(new Label("np").setVisible(false));
        }
    }

}
