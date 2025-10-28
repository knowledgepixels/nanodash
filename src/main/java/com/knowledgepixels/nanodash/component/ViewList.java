package com.knowledgepixels.nanodash.component;

import java.util.List;
import java.util.Set;

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
import com.knowledgepixels.nanodash.User;

public class ViewList extends Panel {

    public ViewList(String markupId, Space space) {
        super(markupId);

        add(new DataView<ResourceView>("views", new ListDataProvider<ResourceView>(space.getViews())) {

            @Override
            protected void populateItem(Item<ResourceView> item) {
                ResourceView view = item.getModelObject();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), space.getId());
                        if (QueryParamField.isMultiPlaceholder(p)) {
                            for (String altId : space.getAltIDs()) {
                                queryRefParams.put("space", altId);
                            }
                        }
                    } else if (paramName.equals(view.getQueryField() + "Np")) {
                        queryRefParams.put(view.getQueryField() + "Np", space.getRootNanopubId());
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
                item.add(QueryResultTable.createComponent("view", queryRef, view, space.getId(), space.getId(), space, 10));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(space.getViews().isEmpty()));
    }

    public ViewList(String markupId, MaintainedResource resource) {
        super(markupId);

        final List<ResourceView> views = resource.getTopLevelViews();
        add(new DataView<ResourceView>("views", new ListDataProvider<ResourceView>(views)) {

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
                    } else if (paramName.equals(view.getQueryField() + "Namespace")) {
                        queryRefParams.put(view.getQueryField() + "Namespace", resource.getNamespace());
                    } else if (paramName.equals(view.getQueryField() + "Np")) {
                        queryRefParams.put(view.getQueryField() + "Np", resource.getNanopubId());
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
                item.add(QueryResultTable.createComponent("view", queryRef, view, resource.getId(), resource.getId(), resource.getSpace(), 10));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(views.isEmpty()));
    }

    public ViewList(String markupId, MaintainedResource resource, String partId, String nanopubId, Set<IRI> partClasses) {
        super(markupId);

        List<ResourceView> views = resource.getPartLevelViews(partClasses);
        add(new DataView<ResourceView>("views", new ListDataProvider<ResourceView>(views)) {

            @Override
            protected void populateItem(Item<ResourceView> item) {
                ResourceView view = item.getModelObject();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), partId);
//                        if (QueryParamField.isMultiPlaceholder(p)) {
//                            for (String altId : resource.getAltIDs()) {
//                                queryRefParams.put("space", altId);
//                            }
//                        }
                    } else if (paramName.equals(view.getQueryField() + "Namespace")) {
                        queryRefParams.put(view.getQueryField() + "Namespace", resource.getNamespace());
                    } else if (paramName.equals(view.getQueryField() + "Np")) {
                        queryRefParams.put(view.getQueryField() + "Np", nanopubId);
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
                item.add(QueryResultTable.createComponent("view", queryRef, view, partId, resource.getId(), resource.getSpace(), 10));
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(views.isEmpty()));
    }


}
