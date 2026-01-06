package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.page.ExplorePage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultPlainParagraph extends QueryResult {

    /**
     * Constructor for QueryResultList.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultPlainParagraph(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        String paragraphTitle = response.getData().isEmpty() ? null : response.getData().get(0).get("title");
        if (paragraphTitle != null && !paragraphTitle.isBlank()) {
            label = paragraphTitle;
        } else if (viewDisplay.getView() != null && viewDisplay.getView().getTitle() != null) {
            label = viewDisplay.getView().getTitle();
        }
        add(new Label("label", label));
        if (viewDisplay.getNanopubId() != null) {
            add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", viewDisplay.getNanopubId())));
        } else {
            add(new Label("np").setVisible(false));
        }

        setOutputMarkupId(true);
        populateComponent();
    }

    @Override
    protected void populateComponent() {
        add(new ListView<>("paragraphs", response.getData()) {
            @Override
            protected void populateItem(ListItem<ApiResponseEntry> item) {
                item.add(new Label("content", item.getModelObject().get("content")).setEscapeModelStrings(false));
            }
        });
    }

}
