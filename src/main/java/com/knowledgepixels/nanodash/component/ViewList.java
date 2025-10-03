package com.knowledgepixels.nanodash.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;

public class ViewList extends Panel {

    public ViewList(String markupId, Space space) {
        super(markupId);

        add(new DataView<GrlcQuery>("views", new ListDataProvider<GrlcQuery>(space.getViews())) {

            @Override
            protected void populateItem(Item<GrlcQuery> item) {
                GrlcQuery query = item.getModelObject();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : query.getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals("SPACE")) {
                        queryRefParams.put("SPACE", space.getId());
                        if (QueryParamField.isMultiPlaceholder(p)) {
                            for (String altId : space.getAltIDs()) {
                                queryRefParams.put("SPACE", altId);
                            }
                        }
                    } else if (paramName.equals("USER_PUBKEY") || QueryParamField.isMultiPlaceholder(p)) {
                        for (IRI userId : space.getUsers()) {
                            for (String memberHash : User.getUserData().getPubkeyhashes(userId, true)) {
                                queryRefParams.put("USER_PUBKEY", memberHash);
                            }
                        }
                    } else if (!QueryParamField.isOptional(p)) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter.</span>").setEscapeModelStrings(false));
                        return;
                    }
                }
                QueryRef queryRef = new QueryRef(query.getQueryId(), queryRefParams);
                item.add(QueryResultTable.createComponent("view", queryRef, false, 10));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(space.getViews().isEmpty()));
    }


}
