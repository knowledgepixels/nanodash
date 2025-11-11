package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;

import java.util.List;
import java.util.Set;

public class ViewList extends Panel {

    public ViewList(String markupId, Space space) {
        super(markupId);

        add(new DataView<ViewDisplay>("views", new ListDataProvider<ViewDisplay>(space.getTopLevelViews())) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                ResourceView view = item.getModelObject().getView();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), space.getId());
                        if (QueryParamField.isMultiPlaceholder(p)) {
                            for (String altId : space.getAltIDs()) {
                                queryRefParams.put(view.getQueryField(), altId);
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
                    } else if (paramName.equals("admin_pubkey") && QueryParamField.isMultiPlaceholder(p)) {
                        for (IRI adminId : space.getAdmins()) {
                            for (String adminHash : User.getUserData().getPubkeyhashes(adminId, true)) {
                                queryRefParams.put("admin_pubkey", adminHash);
                            }
                        }
                    } else if (!QueryParamField.isOptional(p)) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter.</span>").setEscapeModelStrings(false));
                        return;
                    }
                }
                QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
                if (view.getViewType().equals(ResourceView.TABULAR_VIEW)) {
                    item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                            .space(space)
                            .contextId(space.getId())
                            .id(space.getId())
                            .build());
                } else {
                    item.add(QueryResultListBuilder.create("view", queryRef, item.getModelObject()).build());
                }
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(space.getTopLevelViews().isEmpty()));
    }

    public ViewList(String markupId, MaintainedResource resource) {
        super(markupId);

        final List<ViewDisplay> viewDisplays = resource.getTopLevelViews();
        add(new DataView<ViewDisplay>("views", new ListDataProvider<ViewDisplay>(viewDisplays)) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                ResourceView view = item.getModelObject().getView();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), resource.getId());
//                        if (QueryParamField.isMultiPlaceholder(p)) {
//                            for (String altId : resource.getAltIDs()) {
//                                queryRefParams.put(view.getQueryField(), altId);
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
                item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                        .space(resource.getSpace())
                        .id(resource.getId())
                        .contextId(resource.getId())
                        .build());
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(viewDisplays.isEmpty()));
    }

    public ViewList(String markupId, Object spaceOrMaintainedResource, String partId, String nanopubId, Set<IRI> partClasses) {
        super(markupId);

        final String id;
        final List<ViewDisplay> viewDisplays;
        final String namespace;
        final Space space;

        if (spaceOrMaintainedResource instanceof MaintainedResource r) {
            id = r.getId();
            viewDisplays = r.getPartLevelViews(partClasses);
            namespace = r.getNamespace();
            space = r.getSpace();
        } else if (spaceOrMaintainedResource instanceof Space s) {
            id = s.getId();
            viewDisplays = s.getPartLevelViews(partClasses);
            namespace = null;
            space = s;
        } else {
            throw new IllegalArgumentException("Neither MaintainedResource nor Space: " + spaceOrMaintainedResource);
        }

        add(new DataView<ViewDisplay>("views", new ListDataProvider<ViewDisplay>(viewDisplays)) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                ResourceView view = item.getModelObject().getView();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), partId);
//                        if (QueryParamField.isMultiPlaceholder(p)) {
//                            for (String altId : resource.getAltIDs()) {
//                                queryRefParams.put(view.getQueryField(), altId);
//                            }
//                        }
                    } else if (paramName.equals(view.getQueryField() + "Namespace") && namespace != null) {
                        queryRefParams.put(view.getQueryField() + "Namespace", namespace);
                    } else if (paramName.equals(view.getQueryField() + "Np") && nanopubId != null) {
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
                item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                        .space(space)
                        .id(partId)
                        .contextId(id)
                        .build());
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(viewDisplays.isEmpty()));
    }


}
