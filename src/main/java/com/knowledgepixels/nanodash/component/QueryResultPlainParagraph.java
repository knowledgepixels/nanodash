package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

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

        setOutputMarkupId(true);
        populateComponent();
    }

    @Override
    protected void populateComponent() {
        add(new ListView<>("paragraphs", response.getData()) {
            @Override
            protected void populateItem(ListItem<ApiResponseEntry> item) {
                item.add(new Label("title", item.getModelObject().get("title")));
                item.add(new Label("content", item.getModelObject().get("content")).setEscapeModelStrings(false));
            }
        });
    }

}
