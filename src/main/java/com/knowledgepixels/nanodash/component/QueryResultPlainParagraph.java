package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.markup.html.basic.Label;
import org.nanopub.extra.services.ApiResponse;
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
        Label title = new Label("title", response.getData().getFirst().get("title"));
        Label content = new Label("paragraph", response.getData().getFirst().get("content"));
        content.setEscapeModelStrings(false);

        add(title);
        add(content);
    }

}
