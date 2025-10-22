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
import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.ResourceView;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.SpaceQueryView;
import com.knowledgepixels.nanodash.User;

public class ViewList extends Panel {

    public ViewList(String markupId, Space space) {
        super(markupId);

        add(new DataView<SpaceQueryView>("views", new ListDataProvider<SpaceQueryView>(space.getViews())) {

            @Override
            protected void populateItem(Item<SpaceQueryView> item) {
                SpaceQueryView view = item.getModelObject();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals("space")) {
                        queryRefParams.put("space", space.getId());
                        if (QueryParamField.isMultiPlaceholder(p)) {
                            for (String altId : space.getAltIDs()) {
                                queryRefParams.put("space", altId);
                            }
                        }
                    } else if (paramName.equals("user_pubkey") && QueryParamField.isMultiPlaceholder(p)) {
                        for (IRI userId : space.getUsers()) {
                            for (String memberHash : User.getUserData().getPubkeyhashes(userId, true)) {
                                queryRefParams.put("user_pubkey", memberHash);
                            }
                        }
                    } else if (!QueryParamField.isOptional(p)) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter.</span>").setEscapeModelStrings(false));
                        return;
                    }
                }
                QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
                item.add(QueryResultTable.createComponent("view", queryRef, view.getTitle(), 10));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(space.getViews().isEmpty()));
    }

    public ViewList(String markupId, MaintainedResource resource) {
        super(markupId);

        add(new DataView<ResourceView>("views", new ListDataProvider<ResourceView>(resource.getViews())) {

            @Override
            protected void populateItem(Item<ResourceView> item) {
                ResourceView view = item.getModelObject();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), resource.getId());
//                        if (QueryParamField.isMultiPlaceholder(p)) {
//                            for (String altId : resource.getAltIDs()) {
//                                queryRefParams.put("space", altId);
//                            }
//                        }
//                    } else if (paramName.equals("user_pubkey") && QueryParamField.isMultiPlaceholder(p)) {
//                        for (IRI userId : resource.getSpace().getUsers()) {
//                            for (String memberHash : User.getUserData().getPubkeyhashes(userId, true)) {
//                                queryRefParams.put("user_pubkey", memberHash);
//                            }
//                        }
                    } else if (!QueryParamField.isOptional(p)) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter.</span>").setEscapeModelStrings(false));
                        return;
                    }
                }
                QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
                item.add(QueryResultTable.createComponent("view", queryRef, view.getTitle(), 10));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(resource.getViews().isEmpty()));
    }


}
