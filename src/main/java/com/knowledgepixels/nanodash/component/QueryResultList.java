package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.ViewDisplay;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResultList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(QueryResultList.class);
    private RepeatingView listItems;

    QueryResultList(String markupId, GrlcQuery grlcQuery, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getView().getTitle() != null) {
            label = viewDisplay.getView().getTitle();
        }
        add(new Label("label", label));
        listItems = new RepeatingView("listItems");
        for (ApiResponseEntry entry : response.getData()) {
            StringBuilder labelText = new StringBuilder();
            int count = 0;
            for (String header : response.getHeader()) {
                String dataValue = entry.get(header);
                labelText.append(dataValue);
                if (count < response.getHeader().length - 1) {
                    labelText.append(", ");
                }
                count++;

            }
            listItems.add(new Label(listItems.newChildId(), labelText.toString()));
        }
        add(listItems);
    }

}
