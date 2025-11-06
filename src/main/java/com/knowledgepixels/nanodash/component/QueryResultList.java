package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class QueryResultList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryResultList.class);

    private RepeatingView listItems;
    private Label errorLabel;
    private Space space;

    private QueryResultList(String markupId, GrlcQuery q, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId);

        String label = q.getLabel();
        if (viewDisplay.getView().getTitle() != null) {
            label = viewDisplay.getView().getTitle();
        }
        add(new Label("label", label));
        listItems = new RepeatingView("listItems");
        for (ApiResponseEntry entry : response.getData()) {
            StringBuilder labelText = new StringBuilder();
            Iterator<String> keysIterator = entry.getKeys().iterator();
            while (keysIterator.hasNext()) {
                labelText.append(entry.get(Utils.sanitizeHtml(keysIterator.next())));
                if (keysIterator.hasNext()) {
                    labelText.append(", ");
                }
            }
            listItems.add(new Label(listItems.newChildId(), labelText.toString()));
        }
        add(listItems);
    }

    public static Component createListViewComponent(String markupId, QueryRef queryRef, ViewDisplay viewDisplay) {
        final GrlcQuery q = GrlcQuery.get(queryRef);
        ApiResponse response = ApiCache.retrieveResponse(queryRef);
        if (response != null) {
            return new QueryResultList(markupId, q, response, viewDisplay);
        } else {
            return new ApiResultComponent(markupId, queryRef) {

                @Override
                public Component getApiResultComponent(String markupId, ApiResponse response) {
                    return new QueryResultList(markupId, q, response, viewDisplay);
                }

            };
        }
    }

}
