package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Space;

public class ViewList extends Panel {

    public ViewList(String markupId, Space space) {
        super(markupId);

        add(new DataView<GrlcQuery>("views", new ListDataProvider<GrlcQuery>(space.getViews())) {

            @Override
            protected void populateItem(Item<GrlcQuery> item) {
                GrlcQuery query = item.getModelObject();
                item.add(QueryResultTable.createComponent("view", new QueryRef(query.getQueryId()), false));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(space.getViews().isEmpty()));
    }


}
